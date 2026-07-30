# IoT Link SDK

[中文说明](README.zh-CN.md)

This module is an independently developed Android LinkSDK implementation for Alibaba Cloud IoT Platform based on the Alink protocol. It also provides a reusable MQTT client layer for common publish/subscribe scenarios.

The Alink implementation covers device authentication, properties, events, services, OTA, device logs, remote configuration, device jobs, device distribution, gateway topology, sub-devices, tags, device shadow, network diagnostics, and MQTT file upload. The optional remote extension uses Alibaba Cloud IoT secure remote access to provide SSH, SFTP, and device control for Rockchip development boards.

## Requirements

- Android API 24 or newer
- Java 11 / Kotlin JVM target 11
- An Alibaba Cloud IoT product and device identity
- Network permission in the application manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Add the module

```kotlin
dependencies {
    implementation(project(":iot"))
}
```

## Generic MQTT

`androidx.iot.mqtt.Mqtt` is the common MQTT client wrapper. Use it when an application only needs MQTT connectivity and does not need Alink message routing.

```kotlin
val mqtt = Mqtt(context, options, callback)
mqtt.connect(context, actionListener)
mqtt.publish(topic, payload)
mqtt.subscribe(topic, qos = 0)
```

`Options` contains the broker host, client ID, username, password, product key, and device name. `OptionsBuilder` creates Alibaba Cloud IoT authentication options for `connect`, `connwl`, and dynamic registration flows.

## Alink LinkSDK

`androidx.iot.link.LinkSDK` is the high-level Alink protocol entry point. It manages device registration, MQTT lifecycle, standard Topic subscriptions, Alink request/response payloads, and observable device states.

## Connect a device

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

`secureMode = 2` performs dynamic registration when no device secret is stored. The SDK persists registration credentials and reconnects automatically. `remote = true` enables the secure remote-login tunnel.

Connection state is available through `LinkSDK.connectState`; registration state is available through `LinkSDK.registerState`.

## Device logs

The data models are in `androidx.iot.data`:

```kotlin
LinkSDK.publishLog(
    DeviceLogEntry(
        module = "wash-flow",
        logLevel = DeviceLogLevel.ERROR,
        code = "4103",
        logContent = "step recognition failed",
    )
)
```

The SDK requests the cloud log policy after connection and only sends logs when the cloud returns `mode = 1`. A batch is split at Alibaba Cloud's 40-entry limit.

## Remote configuration

The SDK subscribes to configuration push notifications and requests the current product configuration after connection:

```kotlin
LinkSDK.remoteConfigState.value
LinkSDK.publishRemoteConfig() // request again when needed
LinkSDK.replyRemoteConfig(messageId, code = 200)
```

`remoteConfigState` contains the configuration file metadata (`configId`, `configSize`, `sign`, `signMethod`, `url`, and `getType`). The application is responsible for downloading, verifying, and applying the file before reporting the final result code.

## Device jobs

Request a job by ID, or use `$next` / `$list` as defined by the Alink protocol:

```kotlin
LinkSDK.publishJob("$next")
LinkSDK.publishJobStatus(
    taskId = "task-id",
    status = DeviceJobStatus.IN_PROGRESS,
    statusDetails = mapOf("phase" to "download"),
    progress = 50,
)
```

The latest task notification/query result is exposed by `LinkSDK.deviceJobState`. Supported status values are defined by `DeviceJobStatus`.

## Device distribution

The SDK subscribes to `thing.bootstrap.notify`, stores the latest command in `LinkSDK.deviceDistributionState`, and returns the protocol acknowledgement. A device distribution means the application should re-bootstrap or reconnect using the new platform endpoint according to its deployment strategy.

```kotlin
val distribution = LinkSDK.deviceDistributionState.value
LinkSDK.replyDeviceDistribution(messageId, code = 200)
```

## OTA and property messages

Use the public `LinkSDK` methods for OTA version/progress reporting, firmware information requests, property publishing, device enable/disable/delete, and secure-tunnel subscription. Topic names are centralized in `androidx.iot.link.Topics`.

## Gateway and device management

The SDK also provides topology add/delete/query, discovered-device reporting, single and batch sub-device login/logout, desired-property query/deletion, device tag update/deletion, network diagnostics, device shadow update/subscription, and MQTT file-upload init/chunk/cancel APIs.

```kotlin
LinkSDK.publishTopologyGet()
LinkSDK.publishSubDeviceLogin(GatewayDevice(productKey, deviceName))
LinkSDK.updateTags(listOf(DeviceTag("site", "shenzhen")))
LinkSDK.publishShadowUpdate(mapOf("method" to "update", "state" to mapOf("reported" to state)))
```

File-upload methods accept protocol parameter objects so the application can stream and chunk local files safely.

Management protocol responses and device-shadow messages are available from `LinkSDK.managementMessageState`. The SDK subscribes to these replies after connection and clears the state on disconnect.

### Limits and file chunks

- Topology add/delete: at most 30 devices per request.
- Batch sub-device login/logout: at most 50 devices per request.
- File chunks can be published with the typed overload; it Base64-encodes the payload and builds `fileInfo` and `fileBlock` automatically.

```kotlin
LinkSDK.publishFileUploadChunk(
    fileToken = token,
    streamId = streamId,
    fileId = fileId,
    offset = offset,
    bytes = buffer,
)
```

## Remote SSH/SFTP and diagnostics

The `androidx.iot.remote` package contains the Android interactive shell, SSH/SFTP server, logcat access, process metrics, CPU/GPU/NPU information, memory information, camera information, file operations, and system-time commands. Use `androidx.iot.server.FileServer` and `androidx.iot.remote.SSH` only from a controlled administrative workflow.

## Testing

Run the module unit tests from the repository root:

```powershell
.\gradlew.bat :iot:testDebugUnitTest --console=plain
```

The tests validate Alink payloads, topic paths, batching rules, and remote command behavior.

## Security notes

- Do not hard-code product secrets in source control.
- Keep `remote = false` unless secure remote access is explicitly required.
- Validate and authenticate downloaded remote configuration files before applying them.
- Treat job documents and job files as untrusted input.
