#### MQTT

Android MQTT连接,重新编译Service-1.1.1兼容Android高版本服务   
Paho Android Service-1.1.1      
Paho Client Mqtt3-1.1.0

#### 资源

|名字|资源|
|-|-|
|AAR|[下载](https://github.com/RelinRan/IoT/tree/main/aar)|
|GitHub |[查看](https://github.com/RelinRan/IoT)|
|Gitee|[查看](https://gitee.com/relin/IoT)|


#### Maven

1.build.grade | setting.grade

```
repositories {
	...
	maven { url 'https://jitpack.io' }
}
```

2./app/build.grade

```
dependencies {
	implementation 'com.github.RelinRan:IoT:2024.01.05.1'
}
```

#### AndroidManifest.xml

Permission

```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.MANAGE_MEDIA" tools:ignore="ProtectedPermissions" />
```

Service

```
<!-- Mqtt Service -->
<service android:name="org.eclipse.paho.android.service.MqttService" />
```

Provider

```
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileProvider"
    android:exported="false"
    android:grantUriPermissions="true"
    android:permission="android.permission.MANAGE_DOCUMENTS">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/path" />
    <intent-filter>
        <action android:name="android.content.action.DOCUMENTS_PROVIDER" />
    </intent-filter>
</provider>
```

res/xml/path.xml

```
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <root-path
        name="root"
        path="/storage/emulated/0" />
    <files-path
        name="files"
        path="/storage/emulated/0/Android/data/${applicationId}/files" />
    <cache-path
        name="cache"
        path="/storage/emulated/0/Android/data/${applicationId}/cache" />
    <external-path
        name="external"
        path="/storage/emulated/0/Android/data/${applicationId}/external" />
    <external-files-path
        name="Capture"
        path="/storage/emulated/0/Android/data/${applicationId}/files/Capture" />
    <external-cache-path
        name="Pick"
        path="/storage/emulated/0/Android/data/${applicationId}/files/Pick" />
    <external-cache-path
        name="TBS"
        path="/storage/emulated/0/Android/data/${applicationId}/files/TBS" />
</paths>
```
#### 通用Mqtt
连接MQTT
```
MqttOption option = new MqttOption();
option.setHost("xxx");
option.setUserName("xxx");
option.setClientId("xxx");
option.setPassword("xxx");
Mqtt mqtt = Mqtt.initialize(context,option);
long cid = mqtt.addConnectListener(xxx);
long mid = mqtt.addMessageListener(xxx);
mqtt.connect();
```
移除监听
```
Mqtt client = Mqtt.client();
client.remove(cid.mid);
```

#### 设备授权
初始化三元组许可文件

```
License.initialize(LicenseType.PRE_REGISTRATION,"You Project Name","License","triplet.txt");
```
U盘注册三元组（文件名：triplet.txt）

```
{"productKey":"xxx","deviceName":"xxx","deviceSecret":"xxx"}
```

注册三元组许可到设备

```
License license = License.acquire();
params.setDeviceName(xxx);
params.setProductKey(xxx);
params.setDeviceSecret(xxx);
license.granted();
```

检查是否授权许可设备

```
License license = License.acquire();
boolean isLicensed = license.isLicensed();
```

监听三元组信息

```
//当前类 implements OnLicenseListener
license.register(this);

@Override
public void onLicense(String content) {
    License.with(context).unregister();
    //三元组内容
}
```
#### 动态注册

一型一密预注册
```
Dynamic dynamic = new Dynamic();
dynamic.addDynamicListener((topic, payload) -> {
    License.acquire().fromJSON(payload).granted();
    //接下来可以使用授权文件执行连接操作
});
dynamic.register("Your instane id", "product key", "product secret", "device name");
```
一型一密免预注册
```
Dynamic dynamic = new Dynamic();
dynamic.addDynamicListener((topic, payload) -> {
    License.acquire().fromJSON(payload).granted();
    //接下来可以使用授权文件执行连接操作
});
dynamic.regnwl("Your instane id", "product key", "product secret", "device name");
```
#### 阿里物联网

一型一密预注册连接服务

```
Link link = Link.initialize(this, LicenseType.PRE_REGISTRATION);
link.debug(true);
long cid = link.addConnectListener(xxx);
long mid = link.addMessageListener(xxx);
link.connect();
```
一型一密免预注册连接服务

```
Link link = Link.initialize(this, LicenseType.NO_PRE_REGISTRATION);
link.debug(true);
long cid = link.addConnectListener(xxx);
long mid = link.addMessageListener(xxx);
link.connect();
```

连接监听 

```
//当前class implements OnConnectListener

//可跨页面设置监听,不用EventBus等转发信息,页面关闭注意根据id移除监听
long cid = Link.acquire().addMessageListener(this);

@Override
public void onConnectionLost(Throwable cause) {
    
}

@Override
public void onConnectionSuccessful(IMqttToken token) {

}

@Override
public void onConnectionFailed(IMqttToken token, Throwable exception) {

}

//页面关闭时移除监听
Link.acquire().remove(cid);
```

消息监听

```
//当前class implements OnMessageListener

//可跨页面设置监听,不用EventBus等转发信息,页面关闭注意根据id移除监听
long mid = Link.acquire().addMessageListener(this);

@Override
public void onMessageReceived(String topic, MqttMessage message) {
    
}

@Override
public void onMessageDelivered(IMqttDeliveryToken token) {

}

//页面关闭时移除监听
Link.acquire().remove(mid);
```

释放资源

```
//移除监听
link.remove(cid, mid);
//释放资源
link.destroy();
```
#### 日志文件
通用日志
```
//初始化：项目名称/文件夹名称/日志文件前缀名
LogFile log = new LogFile(context,"Your Project Name","Your Dir Name","Log");
//写入内容
log.write("file content");

//设置时间单位
log.setTimeUnit(TimeUnit.HOURS);
//设置第一次维护延迟时间
log.setInitialDelay(0);
//设置文件有效期
log.setExp(7*24);
//开始自动维护检查
log.startScheduled();

//#onDestory()
log.cancel();
```
报错日志
```
//注意：使用这个类需要提前申请文件写入、读取权限，在Android 6.0需要动态申请权限
CrashLog log = CrashLog.initialize(context);//初始化
log.setBugly("Bugly APP ID");//腾讯Bugly APP ID
log.setFolder("YourProjectName","Crash");//文件夹
log.setSuffix("crash");//文件前缀
log.setTimeUnit(TimeUnit.HOURS);//时间单位
log.setInitialDelay(0);//定时任务第一次延迟时间
log.setExp(7*24);//文件过期时间
log.setPeriod(4);//检查频率

log.startScheduled();//开启定时任务

//#destory
log.cancel();//取消所有任务操作
```

#### 日志服务

```
//打印并上报
LogService.d("模块", 200, "日志信息");
//打印不上报
LogService.d(false,"模块", 200, "日志信息");
```
#### 属性上报

```
Map<String,Object> map = new HashMap<>();
map.put("power",1);
Link.api().publishProperty(map);
```
#### 事件上报

```
//参数
Map<String, Object> params = new HashMap<>();
params.put("name", "value");
//functionBlockId 自定义模块id,默认模块为空.
//identifier      属性标识符
Link.api().publishEvent("functionBlockId", "identifier", params);
```
#### OTA
订阅升级主题
```
Link.api().subscribeOTA();
```
平台推送升级
```
@Override
public void onMessageReceived(String topic, MqttMessage message) {
    Alink alink = new Alink(link,License.with(context));
    if (alink.isTopic(Topic.SUB_OTA_UPGRADE, topic)) {
        alink.showOTADialog(context, message);
    }
}
```
用户主动升级
```
//default为模块名称
Link.api().publishOTAGet("default");

@Override
public void onMessageReceived(String topic, MqttMessage message) {
    Alink alink = new Alink(link,License.with(context));
    if (alink.isTopic(Topic.SUB_OTA_FIRMWARE_GET, topic)) {
        boolean isUpgrade = alink.isOTAUpgrade(this, message);
        //自动处理下载，并且有显示进度，然后自动安装重启
        alink.showOTADialog(context, message);
    }
}
```

