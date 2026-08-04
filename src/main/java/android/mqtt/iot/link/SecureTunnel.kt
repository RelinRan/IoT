package androidx.iot.link

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.compose.runtime.MutableState
import androidx.iot.data.TunnelProxy
import androidx.iot.mqtt.Options
import androidx.iot.remote.AndroidInteractiveShellFactory
import androidx.iot.remote.SSH
import androidx.iot.remote.SecureTunnelSshBridge
import com.google.gson.Gson
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal fun shouldReuseSecureTunnel(
    activeTunnelId: String?,
    incomingTunnelId: String,
    operation: String?,
): Boolean {
    return activeTunnelId == incomingTunnelId &&
        operation.equals(SecureTunnelSshBridge.OPERATION_CONNECT, ignoreCase = true)
}

internal class SecureTunnel(
    private val tunnelState: MutableState<TunnelProxy?>,
    private val publishSecureTunnelProxy: () -> Unit,
    private val gson: Gson = Gson(),
) {
    private var secureTunnelBridge: SecureTunnelSshBridge? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val bridgeExecutor = Executors.newSingleThreadExecutor()
    private var refreshTask: ScheduledFuture<*>? = null
    private var activeTunnelId: String? = null
    private var lastStartedRemoteApp: AndroidInteractiveShellFactory.RunningAppInfo? = null
    private var lastStartedRemoteAppAt: Long = 0L

    fun handlePayload(payload: String, source: String, options: Options) {
        val json = runCatching { gson.fromJson(payload, com.google.gson.JsonObject::class.java) }.getOrNull()
        val code = json?.get("code")?.takeIf { !it.isJsonNull }?.asInt
        if (code != null && code != 0 && code != 200) {
            val message = json.get("message")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            Log.i(TAG, "secure tunnel $source failed:code=$code message=$message")
            return
        }

        val tunnelJson = when {
            json?.has("data") == true && json.get("data").isJsonObject -> json.getAsJsonObject("data")
            else -> json
        }
        val tunnel = runCatching { gson.fromJson(tunnelJson, TunnelProxy::class.java) }.getOrNull()
        if (tunnel == null) {
            Log.i(TAG, "secure tunnel $source parse failed:$payload")
            return
        }
        if (tunnel.operation.equals(SecureTunnelSshBridge.OPERATION_CLOSE, ignoreCase = true)) {
            tunnelState.value = null
            bridgeExecutor.execute(::closeBridge)
        } else {
            tunnelState.value = tunnel
            bridgeExecutor.execute { connect(tunnel, options) }
        }
    }

    fun close() {
        tunnelState.value = null
        bridgeExecutor.execute(::closeBridge)
    }

    private fun closeBridge() {
        cancelRefresh()
        secureTunnelBridge?.close()
        secureTunnelBridge = null
        activeTunnelId = null
    }

    private fun connect(proxy: TunnelProxy, options: Options) {
        Log.i(TAG, "secure tunnel proxy received:${proxy.host}:${proxy.port}${proxy.path}")
        if (proxy.operation.orEmpty().equals(SecureTunnelSshBridge.OPERATION_CLOSE, ignoreCase = true)) {
            Log.i(TAG, "secure tunnel close received, close ssh bridge")
            closeBridge()
            return
        }
        if (shouldReuseSecureTunnel(activeTunnelId, proxy.tunnel_id, proxy.operation) && secureTunnelBridge != null) {
            Log.i(TAG, "secure tunnel duplicate connect ignored:tunnelId=${proxy.tunnel_id}")
            scheduleRefresh(proxy)
            return
        }

        val sshUsername = options.deviceName
        val sshPassword = options.deviceName.takeLast(6)
        Log.i(
            TAG,
            "secure tunnel endpoint(device websocket):${proxy.host}:${proxy.port}${proxy.path} SSH login username:$sshUsername"
        )
        Log.i(TAG, lanSshClientConnectMessage(sshUsername))
        closeBridge()
        val packageManager = options.context.packageManager
        val bridge = SecureTunnelSshBridge(
            sshUsername = sshUsername,
            sshPassword = sshPassword,
            appInfoResolver = { packageName ->
                runCatching {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    packageInfo.toRemoteAppInfo(packageManager)
                }.getOrNull()
            },
            appListResolver = {
                runCatching {
                    packageManager.getInstalledPackagesCompat()
                        .map { packageInfo -> packageInfo.toRemoteAppInfo(packageManager) }
                }.getOrDefault(emptyList())
            },
            appLaunchActivityResolver = { packageName ->
                runCatching {
                    packageManager.getLaunchIntentForPackage(packageName)
                        ?.component
                        ?.flattenToShortString()
                }.getOrNull()
            },
            appStartResolver = { packageName, activity ->
                startRemoteApp(packageName, activity, options)
            },
            runningAppResolver = {
                resolveRunningApps(options)
            },
            cameraResolver = {
                resolveCameraInfo(options)
            },
            volumeResolver = {
                resolveVolumeInfo(options)
            },
            listener = object : SecureTunnelSshBridge.Listener {
                override fun onOpen() {
                    Log.i(TAG, "secure tunnel ssh bridge open")
                }

                override fun onClosed() {
                    Log.i(TAG, "secure tunnel ssh bridge closed")
                }

                override fun onFailure(t: Throwable) {
                    Log.i(TAG, "secure tunnel ssh bridge failure:${t.message}", t)
                }
            }
        )
        secureTunnelBridge = bridge
        activeTunnelId = proxy.tunnel_id
        bridge.open(proxy)
        if (!bridge.isOpen()) {
            secureTunnelBridge = null
            activeTunnelId = null
            return
        }
        scheduleRefresh(proxy)
    }

    private fun startRemoteApp(
        packageName: String,
        activity: String?,
        options: Options,
    ): AndroidInteractiveShellFactory.AppStartResult {
        return runCatching {
            val intent = if (activity.isNullOrBlank()) {
                options.context.packageManager.getLaunchIntentForPackage(packageName)
                    ?: return AndroidInteractiveShellFactory.AppStartResult(false, "launcher activity not found")
            } else {
                val component = when {
                    activity.startsWith("/") -> ComponentName(packageName, packageName + activity)
                    "/" in activity -> ComponentName.unflattenFromString(activity)
                    activity.startsWith(".") -> ComponentName(packageName, packageName + activity)
                    "." in activity -> ComponentName(packageName, activity)
                    else -> ComponentName(packageName, "$packageName.$activity")
                } ?: return AndroidInteractiveShellFactory.AppStartResult(false, "invalid activity: $activity")
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            options.context.startActivity(intent)
            val component = intent.component?.flattenToShortString() ?: packageName
            lastStartedRemoteApp = AndroidInteractiveShellFactory.RunningAppInfo(
                packageName = packageName,
                processName = packageName,
                state = "FOREGROUND",
                source = "last-started",
            )
            lastStartedRemoteAppAt = System.currentTimeMillis()
            AndroidInteractiveShellFactory.AppStartResult(true, component)
        }.getOrElse { error ->
            AndroidInteractiveShellFactory.AppStartResult(false, error.message ?: error.javaClass.simpleName)
        }
    }

    private fun resolveCameraInfo(options: Options): List<AndroidInteractiveShellFactory.CameraInfo> {
        val manager = options.context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()
        return runCatching {
            manager.cameraIdList.map { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
                    else -> "-"
                }
                val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)?.toString() ?: "-"
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?.joinToString(",") { capability -> cameraCapabilityName(capability) }
                    ?: "-"
                val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    ?.let(::cameraHardwareLevelName)
                    ?: "-"
                val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val autofocus = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                    ?.joinToString(",") { mode -> cameraAfModeName(mode) }
                    ?: "-"
                val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?.joinToString(",") { range -> formatRange(range) }
                    ?: "-"
                AndroidInteractiveShellFactory.CameraInfo(
                    id = id,
                    facing = facing,
                    orientation = orientation,
                    hardwareLevel = level,
                    flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)?.toString() ?: "-",
                    autofocus = autofocus,
                    fpsRanges = fpsRanges,
                    photoSizes = streamMap.formatOutputSizes(ImageFormat.JPEG),
                    videoSizes = streamMap.classOutputSizes(MediaRecorder::class.java),
                    capabilities = capabilities,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun resolveVolumeInfo(options: Options): List<AndroidInteractiveShellFactory.VolumeInfo> {
        val audioManager = options.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return emptyList()
        val streams = listOf(
            "voice-call" to AudioManager.STREAM_VOICE_CALL,
            "system" to AudioManager.STREAM_SYSTEM,
            "ring" to AudioManager.STREAM_RING,
            "music" to AudioManager.STREAM_MUSIC,
            "alarm" to AudioManager.STREAM_ALARM,
            "notification" to AudioManager.STREAM_NOTIFICATION,
        )
        return streams.map { (name, stream) ->
            AndroidInteractiveShellFactory.VolumeInfo(
                stream = name,
                min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audioManager.getStreamMinVolume(stream).toString() else "0",
                current = audioManager.getStreamVolume(stream).toString(),
                max = audioManager.getStreamMaxVolume(stream).toString(),
                muted = runCatching { audioManager.isStreamMute(stream).toString() }.getOrDefault("-"),
            )
        }
    }

    private fun resolveRunningApps(options: Options): List<AndroidInteractiveShellFactory.RunningAppInfo> {
        val activityManager = options.context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return emptyList()
        val rows = mutableListOf<AndroidInteractiveShellFactory.RunningAppInfo>()
        runCatching {
            activityManager.runningAppProcesses.orEmpty().forEach { process ->
                val packages = process.pkgList?.filter { it.isNotBlank() }.orEmpty()
                packages.forEach { packageName ->
                    rows += AndroidInteractiveShellFactory.RunningAppInfo(
                        packageName = packageName,
                        pid = process.pid.takeIf { it > 0 }?.toString().orEmpty(),
                        processName = process.processName ?: packageName,
                        state = process.importance.toRunningState(),
                        source = "activity-manager",
                    )
                }
            }
        }
        @Suppress("DEPRECATION")
        runCatching {
            activityManager.getRunningServices(Int.MAX_VALUE).orEmpty().forEach { service ->
                val component = service.service ?: return@forEach
                rows += AndroidInteractiveShellFactory.RunningAppInfo(
                    packageName = component.packageName,
                    pid = service.pid.takeIf { it > 0 }?.toString().orEmpty(),
                    processName = service.process ?: component.packageName,
                    state = "SERVICE",
                    source = "activity-service",
                )
            }
        }
        val lastStarted = lastStartedRemoteApp
        if (lastStarted != null && System.currentTimeMillis() - lastStartedRemoteAppAt <= LAST_STARTED_APP_VISIBLE_MILLIS) {
            rows += enrichLastStartedRemoteApp(activityManager, lastStarted)
        }
        return rows.distinctBy { "${it.packageName}:${it.pid}:${it.processName}:${it.source}" }
    }

    private fun enrichLastStartedRemoteApp(
        activityManager: ActivityManager,
        app: AndroidInteractiveShellFactory.RunningAppInfo,
    ): AndroidInteractiveShellFactory.RunningAppInfo {
        val process = activityManager.runningAppProcesses.orEmpty().firstOrNull { process ->
            process.processName == app.packageName || process.pkgList?.contains(app.packageName) == true
        } ?: return app
        return app.copy(
            pid = process.pid.takeIf { it > 0 }?.toString().orEmpty(),
            processName = process.processName ?: app.processName,
            state = process.importance.toRunningState(),
        )
    }

    private fun lanSshClientConnectMessage(username: String): String {
        val addresses = localIpv4Addresses()
        val commands = if (addresses.isEmpty()) {
            "ssh -p ${SSH.DEFAULT_PORT} $username@<device-lan-ip>"
        } else {
            addresses.joinToString(separator = " ; ") { address ->
                "ssh -p ${SSH.DEFAULT_PORT} $username@$address"
            }
        }
        return "LAN SSH client connect:$commands username:$username password:******"
    }

    private fun localIpv4Addresses(): List<String> {
        return runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { networkInterface ->
                    networkInterface.isUp && !networkInterface.isLoopback && !networkInterface.isVirtual
                }
                .flatMap { networkInterface ->
                    Collections.list(networkInterface.inetAddresses).asSequence()
                }
                .filterIsInstance<Inet4Address>()
                .map { address -> address.hostAddress }
                .filter { address -> !address.isNullOrBlank() && !address.startsWith("127.") }
                .distinct()
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun scheduleRefresh(proxy: TunnelProxy) {
        cancelRefresh()
        val refreshDelaySeconds = secureTunnelRefreshDelaySeconds(proxy.token_expire)
        refreshTask = scheduler.schedule(
            {
                Log.i(
                    TAG,
                    "secure tunnel token refresh request:tunnelId=${proxy.tunnel_id} delay=${refreshDelaySeconds}s"
                )
                publishSecureTunnelProxy()
            },
            refreshDelaySeconds,
            TimeUnit.SECONDS
        )
    }

    private fun cancelRefresh() {
        refreshTask?.cancel(false)
        refreshTask = null
    }

    private fun StreamConfigurationMap?.formatOutputSizes(format: Int): String {
        return this?.getOutputSizes(format)
            ?.sortedByDescending { size -> size.width.toLong() * size.height }
            ?.take(6)
            ?.joinToString(",") { size -> formatSize(size) }
            ?.ifBlank { "-" }
            ?: "-"
    }

    private fun StreamConfigurationMap?.classOutputSizes(clazz: Class<*>): String {
        return runCatching {
            this?.getOutputSizes(clazz)
                ?.sortedByDescending { size -> size.width.toLong() * size.height }
                ?.take(6)
                ?.joinToString(",") { size -> formatSize(size) }
                ?.ifBlank { "-" }
                ?: "-"
        }.getOrDefault("-")
    }

    private fun PackageManager.getInstalledPackagesCompat(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getInstalledPackages(0)
        }
    }

    private fun PackageInfo.toRemoteAppInfo(packageManager: PackageManager): AndroidInteractiveShellFactory.AppInfo {
        val appInfo = applicationInfo
        return AndroidInteractiveShellFactory.AppInfo(
            packageName = packageName,
            appName = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: "-",
            versionName = versionName ?: "-",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                versionCode.toString()
            },
            apkPath = appInfo?.sourceDir ?: "-",
            firstInstallTime = formatAppTime(firstInstallTime),
            lastUpdateTime = formatAppTime(lastUpdateTime),
        )
    }

    private fun Int.toRunningState(): String {
        return when {
            this <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "FOREGROUND"
            this <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "SERVICE"
            else -> "BACKGROUND"
        }
    }

    companion object {
        private const val TAG = "LinkSDK"
        private const val SECURE_TUNNEL_REFRESH_BEFORE_EXPIRE_SECONDS = 5 * 60
        private const val SECURE_TUNNEL_MAX_REFRESH_INTERVAL_SECONDS = 5 * 24 * 60 * 60
        private const val LAST_STARTED_APP_VISIBLE_MILLIS = 10 * 60 * 1000L

        fun secureTunnelRefreshDelaySeconds(tokenExpireSeconds: Int): Long {
            if (tokenExpireSeconds <= 0) return 1L
            val beforeExpire = tokenExpireSeconds - SECURE_TUNNEL_REFRESH_BEFORE_EXPIRE_SECONDS
            return when {
                beforeExpire <= 0 -> 1L
                beforeExpire > SECURE_TUNNEL_MAX_REFRESH_INTERVAL_SECONDS -> SECURE_TUNNEL_MAX_REFRESH_INTERVAL_SECONDS.toLong()
                else -> beforeExpire.toLong()
            }
        }

        private fun formatSize(size: Size): String {
            return "${size.width}x${size.height}"
        }

        private fun formatRange(range: Range<Int>): String {
            return "${range.lower}-${range.upper}"
        }

        private fun cameraAfModeName(mode: Int): String {
            return when (mode) {
                CameraCharacteristics.CONTROL_AF_MODE_OFF -> "off"
                CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "auto"
                CameraCharacteristics.CONTROL_AF_MODE_MACRO -> "macro"
                CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "continuous-video"
                CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "continuous-picture"
                CameraCharacteristics.CONTROL_AF_MODE_EDOF -> "edof"
                else -> mode.toString()
            }
        }

        private fun cameraCapabilityName(capability: Int): String {
            return when (capability) {
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "backward-compatible"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "manual-sensor"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "manual-post-processing"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "raw"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "private-reprocessing"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "read-sensor-settings"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "burst"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "yuv-reprocessing"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "depth"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "high-speed-video"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> "motion-tracking"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "logical-multi-camera"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> "monochrome"
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA -> "secure-image-data"
                else -> capability.toString()
            }
        }

        private fun cameraHardwareLevelName(level: Int): String {
            return when (level) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "legacy"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "limited"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "full"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "level-3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "external"
                else -> level.toString()
            }
        }

        private fun formatAppTime(timeMillis: Long): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timeMillis))
        }
    }
}
