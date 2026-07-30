package androidx.iot.server

import androidx.compose.runtime.mutableStateOf
import androidx.iot.data.FileInfo
import androidx.iot.utils.Files
import androidx.iot.utils.Files.unzipTo
import androidx.iot.utils.Store
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.Locale
import java.util.logging.Logger

/**
 * 文件服务器
 */
object FileServer {

    private val log: Logger = Logger.getLogger(FileServer::class.java.name)
    private var project: String = "iot"
    private var dir: String = "files"
    private var receivedPort = 9999
    private var discoveryPort = 9998
    private lateinit var socket: DatagramSocket
    private lateinit var serverSocket: ServerSocket
    private lateinit var scope: CoroutineScope
    private val gson = Gson()
    private var open = false
    var restartState = mutableStateOf(false)

    /**
     * 初始化文件服务器
     * @param project 项目名称
     * @param dir 文件目录
     * @param port 端口号
     */
    fun initialize(project: String, dir: String, port: Int) {
        this.project = project
        this.dir = dir
        this.receivedPort = port
    }

    /**
     * 打开文件服务器
     */
    fun open() {
        println("文件服务器打开")
        open = true
        scope = CoroutineScope(Dispatchers.IO)

        // 启动TCP文件接收服务
        scope.launch {
            try {
                serverSocket = ServerSocket(receivedPort)
                log.info("TCP文件服务器启动，端口: $receivedPort")

                while (open) {
                    try {
                        val clientSocket = serverSocket.accept()
                        log.info("新的文件传输连接: ${clientSocket.inetAddress}")
                        // 启动新的协程处理客户端连接
                        scope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: IOException) {
                        if (open) {
                            log.info("TCP服务器接受连接错误: ${e.message}")
                        }
                    }
                }
            } catch (e: IOException) {
                log.info("TCP服务器启动失败: ${e.message}")
            }
        }
        // 启动设备发现服务（保持UDP）
        scope.launch {
            startDiscoveryService()
        }
    }

    /**
     * 处理客户端连接
     */
    private fun handleClient(clientSocket: Socket) {
        try {
            val inputStream = clientSocket.getInputStream()
            while (open && !clientSocket.isClosed) {
                // 接收文件信息长度 (4字节)
                val lengthBuf = ByteArray(4)
                var bytesRead = 0
                while (bytesRead < 4) {
                    val read = inputStream.read(lengthBuf, bytesRead, 4 - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }
                if (bytesRead < 4) {
                    break
                }
                val infoLength = ByteBuffer.wrap(lengthBuf).getInt()
                // 防御：文件信息 JSON 不应超过 4KB，超限拒绝（防脏数据/恶意请求导致 OOM）
                if (infoLength <= 0 || infoLength > 4096) {
                    log.info("文件信息长度异常: $infoLength，拒绝连接")
                    break
                }
                // 接收文件信息
                val infoBuf = ByteArray(infoLength)
                bytesRead = 0
                while (bytesRead < infoLength) {
                    val read = inputStream.read(infoBuf, bytesRead, infoLength - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }

                if (bytesRead < infoLength) {
                    log.info("无法完整读取文件信息")
                    break
                }

                val info = String(infoBuf, 0, bytesRead)
                val fileInfo = gson.fromJson(info, FileInfo::class.java)
                log.info("文件信息: $info")
                restartState.value = false

                // 防御：文件大小不应超过 512MB，超限拒绝（防脏数据/恶意请求导致 OOM）
                if (fileInfo.size <= 0 || fileInfo.size > 512 * 1024 * 1024) {
                    log.info("文件大小异常: ${fileInfo.size}，拒绝接收")
                    break
                }

                // 流式接收文件到磁盘，避免大文件一次性加载到内存导致 OOM
                val receivedFile = File(Store.getExternalStorageDir(project, "tmp"), fileInfo.name)
                receivedFile.parentFile?.mkdirs()
                var totalRead = 0
                try {
                    receivedFile.outputStream().use { fileOut ->
                        val buf = ByteArray(8192)
                        while (totalRead < fileInfo.size) {
                            val toRead = minOf(buf.size, fileInfo.size - totalRead)
                            val read = inputStream.read(buf, 0, toRead)
                            if (read == -1) break
                            fileOut.write(buf, 0, read)
                            totalRead += read
                        }
                        fileOut.flush()
                    }
                } catch (e: Exception) {
                    log.info("文件写入错误: ${e.message}")
                    receivedFile.delete()
                }

                if (totalRead == fileInfo.size) {
                    // 文件已写入磁盘，直接用文件路径处理（不加载到内存）
                    saveFile(fileInfo, receivedFile)
                } else {
                    log.info("文件数据不完整，期望: ${fileInfo.size} 字节, 实际: $totalRead 字节")
                    receivedFile.delete()
                }
            }
        } catch (e: Exception) {
            log.info("处理客户端连接错误: ${e.message}")
        } finally {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                log.info("关闭客户端连接错误: ${e.message}")
            }
        }
    }

    /**
     * 保存文件（从临时文件移动/解压到目标目录）
     * @param fileInfo 文件信息
     * @param file 已写入的临时文件
     */
    private fun saveFile(fileInfo: FileInfo, file: File) {
        val tmpDir = Store.getExternalStorageDir(project, "tmp")
        if (fileInfo.name == "studio_models.zip") {
            file.unzipTo(tmpDir)
            log.info("文件解压: ${file.absolutePath}")
            restartState.value = true
        }
        if (fileInfo.name == "float_models.zip") {
            file.unzipTo(
                Store.getExternalStorageDir(project, "model")
            )
            log.info("文件解压: ${file.absolutePath}")
            restartState.value = true
        } else {
            var dirName = "files"
            if (fileInfo.name.endsWith(".float32") || fileInfo.name == "version.txt") {
                dirName = dir
            } else if (fileInfo.name.endsWith(".task") || fileInfo.name.endsWith(".tflite")) {
                dirName = "tmp"
            } else if (fileInfo.name.endsWith(".apk")) {
                dirName = "apk"
            }
            val targetDir = Store.getExternalStorageDir(project, dirName)
            val targetFile = File(targetDir, fileInfo.name)
            if (file.absolutePath != targetFile.absolutePath) {
                file.copyTo(targetFile, overwrite = true)
                file.delete()
            }
            log.info("文件保存: ${targetFile.absolutePath}")
        }
    }

    /**
     * 启动设备发现服务
     */
    /**
     * 启动设备发现服务
     */
    private fun startDiscoveryService() {
        var discoverySocket: DatagramSocket? = null
        try {
            discoverySocket = DatagramSocket(discoveryPort)
            discoverySocket.broadcast = true
            discoverySocket.soTimeout = 5000
            while (open) {
                try {
                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)
                    discoverySocket.receive(packet)

                    val receivedData = String(packet.data, 0, packet.length)
                    log.info("设备发现服务收到: $receivedData")
                    if (receivedData == "DISCOVER_DEVICES") {
                        val response =
                            "DEVICE_INFO:${getDeviceId()}(${getLocalIpAddress()}:${receivedPort})"
                        val responseData = response.toByteArray()
                        //发送响应
                        val responsePacket = DatagramPacket(
                            responseData,
                            responseData.size,
                            packet.address,
                            packet.port
                        )
                        discoverySocket.send(responsePacket)
                        log.info("设备发现服务响应已发送: $response to ${packet.address}:${packet.port}")
                    }
                } catch (e: Exception) {
                    // 忽略超时异常，继续监听
                    if (e !is java.net.SocketTimeoutException) {
                        log.info("设备发现服务错误: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            log.info("设备发现服务启动失败: ${e.message}")
        } finally {
            // 确保socket被正确关闭
            discoverySocket?.close()
            log.info("设备发现服务已关闭")
        }
    }

    private fun getDeviceId(): String {
        var macAddress = ""
        try {
            val reader = BufferedReader(FileReader("/sys/class/net/eth0/address"))
            macAddress = reader.readLine()
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return macAddress.replace(":", "").uppercase(Locale.getDefault())
    }

    /**
     * 获取本机IP地址
     */
    private fun getLocalIpAddress(): String {
        try {
            val sockets = java.net.NetworkInterface.getNetworkInterfaces()
            for (intf in sockets) {
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') == -1) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "127.0.0.1"
    }


    /**
     * 关闭文件服务器
     */
    fun close() {
        open = false

        // 关闭UDP socket
        if (::socket.isInitialized && !socket.isClosed) {
            socket.close()
        }

        // 关闭TCP server socket
        if (::serverSocket.isInitialized && !serverSocket.isClosed) {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                log.info("关闭TCP服务器错误: ${e.message}")
            }
        }

        log.info("文件服务器关闭")
    }

}