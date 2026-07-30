# IoT Link SDK 中文说明

## 网关和设备管理

SDK 还提供拓扑关系增删查、发现设备上报、子设备单个/批量上下线、期望属性查询/删除、设备标签更新/删除、网络诊断、设备影子以及 MQTT 文件上传初始化/分片/取消接口。

```kotlin
LinkSDK.publishTopologyGet()
LinkSDK.publishSubDeviceLogin(GatewayDevice(productKey, deviceName))
LinkSDK.updateTags(listOf(DeviceTag("site", "shenzhen")))
LinkSDK.publishShadowUpdate(mapOf("method" to "update", "state" to mapOf("reported" to state)))
```

管理协议响应和设备影子下行消息统一通过 `LinkSDK.managementMessageState` 获取；SDK 连接成功后自动订阅，断开连接时自动清空。

### 数量限制和文件分片

- 拓扑关系添加/删除：单次最多 30 台设备。
- 子设备批量上线/下线：单次最多 50 台设备。
- 文件分片可使用强类型重载，SDK 会自动 Base64 编码数据并构建 `fileInfo`、`fileBlock` 参数。

```kotlin
LinkSDK.publishFileUploadChunk(
    fileToken = token,
    streamId = streamId,
    fileId = fileId,
    offset = offset,
    bytes = buffer,
)
```

`iot` 模块是 Android 设备端物联网连接层，包含阿里云 IoT MQTT 接入、Alink 协议、设备日志、远程配置、设备任务、设备分发、OTA、远程安全隧道、系统监控以及 SSH/SFTP 服务。模块基于阿里云物联网安全远程能力进行自定义实现，支持 Rockchip 开发板的 SSH 访问、SFTP 文件管理和设备控制。

## 环境要求

- Android API 24 及以上
- Java 11 / Kotlin JVM 目标 11
- 已创建阿里云 IoT 产品和设备
- Manifest 中声明网络权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 引入模块

```kotlin
dependencies {
    implementation(project(":iot"))
}
```

## 连接设备

```kotlin
LinkSDK.initialize(
    context = applicationContext,
    productKey = PRODUCT_KEY,
    productSecret = PRODUCT_SECRET,
    deviceName = DEVICE_NAME,
    secureMode = 2,
    authType = "register",
    remote = false,
)
```

`secureMode = 2` 表示没有设备密钥时执行动态注册。注册凭证会持久化保存，后续自动重连。`remote = true` 时启用安全远程登录隧道。

连接状态通过 `LinkSDK.connectState` 获取，注册状态通过 `LinkSDK.registerState` 获取。

## 设备日志

日志数据类位于 `androidx.iot.data`：

```kotlin
LinkSDK.publishLog(
    DeviceLogEntry(
        module = "wash-flow",
        logLevel = DeviceLogLevel.ERROR,
        code = "4103",
        logContent = "步骤识别失败",
    )
)
```

连接成功后 SDK 会请求云端日志策略，只有云端返回 `mode = 1` 时才会上报日志。单次上报超过 40 条时会自动拆分。

## 远程配置

SDK 连接后自动订阅配置推送并请求当前产品配置：

```kotlin
LinkSDK.remoteConfigState.value
LinkSDK.publishRemoteConfig() // 需要时再次请求
LinkSDK.replyRemoteConfig(messageId, code = 200)
```

`remoteConfigState` 包含配置文件描述信息：`configId`、`configSize`、`sign`、`signMethod`、`url` 和 `getType`。应用负责下载、验签和应用配置，然后上报最终结果码。

## 设备任务

可以按任务 ID 查询，也可以使用协议规定的 `$next` / `$list`：

```kotlin
LinkSDK.publishJob("$next")
LinkSDK.publishJobStatus(
    taskId = "task-id",
    status = DeviceJobStatus.IN_PROGRESS,
    statusDetails = mapOf("phase" to "download"),
    progress = 50,
)
```

最新任务通知和查询结果通过 `LinkSDK.deviceJobState` 获取，状态枚举由 `DeviceJobStatus` 定义。

## 设备分发

SDK 会订阅 `thing.bootstrap.notify`，将最新分发命令保存到 `LinkSDK.deviceDistributionState`，并返回协议确认。设备分发后，应用应根据部署策略重新请求 Bootstrap 接入点或重新连接新的平台实例。

```kotlin
val distribution = LinkSDK.deviceDistributionState.value
LinkSDK.replyDeviceDistribution(messageId, code = 200)
```

## OTA 和属性消息

通过 `LinkSDK` 公共方法完成 OTA 版本/进度上报、固件信息请求、属性上报、设备启用/禁用/删除以及安全隧道订阅。Topic 统一由 `androidx.iot.link.Topics` 管理。

## SSH/SFTP 和设备监控

`androidx.iot.remote` 包提供 Android 交互式 Shell、SSH/SFTP 服务、logcat、进程指标、CPU/GPU/NPU、内存、摄像头信息、文件操作和系统时间命令。请仅在受控的管理流程中启用 `androidx.iot.server.FileServer` 和 `androidx.iot.remote.SSH`。

## 测试

在项目根目录执行：

```powershell
.\gradlew.bat :iot:testDebugUnitTest --console=plain
```

测试覆盖 Alink 负载、Topic、日志分批以及远程命令行为。

## 安全注意事项

- 不要将产品密钥和设备密钥提交到代码仓库。
- 除非明确需要远程维护，否则保持 `remote = false`。
- 应用远程配置前必须校验下载文件和签名。
- 将任务文档和任务文件视为不可信输入。
