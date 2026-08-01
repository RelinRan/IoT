# IoT Link SDK 中文说明

[English](README.md)

> 自动发布：推送 `v1.0.0` 这类标签会触发 GitHub Actions，构建 Release AAR、保存详细 Gradle 构建日志和 SHA-256 校验文件，并将 `iot-1.0.0.aar` 发布到 GitHub Release 供用户下载。

本模块主要用于基于阿里云物联网 Alink 协议自主开发 Android 设备端 LinkSDK，同时提供通用 MQTT 发布、订阅和连接能力。

自主 LinkSDK 已覆盖设备认证、属性/事件/服务、OTA、设备日志、远程配置、设备任务、设备分发、网关拓扑、子设备、设备标签、设备影子、网络诊断和 MQTT 文件上传。可选的远程扩展基于阿里云物联网安全远程能力，为 Rockchip 开发板提供 SSH、SFTP 和设备控制。

## 通用 MQTT

`android.mqtt.iot.mqtt.Mqtt` 是通用 MQTT 客户端封装。仅需要 MQTT 连接、发布和订阅，而不需要 Alink 消息路由时，可直接使用该类。

```kotlin
val mqtt = Mqtt(context, options, callback)
mqtt.connect(context, actionListener)
mqtt.publish(topic, payload)
mqtt.subscribe(topic, qos = 0)
```

`Options` 包含 Broker 地址、客户端 ID、用户名、密码、产品 Key 和设备名称；`OptionsBuilder` 可创建阿里云 IoT 的 `connect`、`connwl` 和动态注册认证参数。

## Alink LinkSDK

`android.mqtt.iot.link.LinkSDK` 是 Alink 协议高级入口，负责设备注册、MQTT 生命周期、标准 Topic 订阅、Alink 请求/响应负载和可观察的设备状态。

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

日志数据类位于 `android.mqtt.iot.data`：

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

通过 `LinkSDK` 公共方法完成 OTA 版本/进度上报、固件信息请求、属性上报、设备启用/禁用/删除以及安全隧道订阅。Topic 统一由 `android.mqtt.iot.link.Topics` 管理。

## SSH/SFTP 和设备监控

`android.mqtt.iot.remote` 包提供 Android 交互式 Shell、SSH/SFTP 服务、logcat、进程指标、CPU/GPU/NPU、内存、摄像头信息、文件操作和系统时间命令。请仅在受控的管理流程中启用 `android.mqtt.iot.server.FileServer` 和 `android.mqtt.iot.remote.SSH`。

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
