package androidx.iot.aiot;

import android.content.Context;
import android.util.Log;

import androidx.iot.mqtt.Mqtt;
import androidx.iot.mqtt.MqttOptions;

import org.eclipse.paho.client.mqttv3.IMqttToken;

/**
 * 阿里物联网 - Link客户端（MQTT）
 */
public class Link extends Mqtt {

    public final static String TAG = Link.class.getSimpleName();
    /**
     * 阿里物联网
     */
    private static Link link;
    /**
     * 授权文件
     */
    private License license;
    /**
     * 阿里物联网服务api
     */
    private Alink alink;

    private Link(Context context, String url, License license) {
        setLicense(license);
        LicenseType type = license.getType();
        Options options;
        if (type == LicenseType.PRE_REGISTRATION) {
            options = new Options().connect(license.getProductKey(), license.getDeviceName(), license.getDeviceSecret());
        } else if (type == LicenseType.NO_PRE_REGISTRATION) {
            options = new Options().connwl(license.getClientId(), license.getProductKey(), license.getDeviceName(), license.getDeviceToken());
        } else {
            options = new Options().connect(license.getProductKey(), license.getDeviceName(), license.getDeviceSecret());
        }
        if (options != null) {
            if (isDebug()) {
                Log.d(TAG, license.toJSONString());
            }
            String clientId = options.getClientId();
            String userName = options.getUsername();
            String passWord = options.getPassword();
            setMqttAndroidClientOptions(context, new MqttOptions(url, clientId, userName, passWord));
        } else {
            Log.e(TAG, "initialize failed license parameter incorrect");
        }
    }

    /**
     * 设置授权
     *
     * @param license
     */
    public void setLicense(License license) {
        this.license = license;
    }

    /**
     * 获取授权文件
     *
     * @return
     */
    public License getLicense() {
        return license;
    }

    /**
     * 获取阿里物联网MQTT
     *
     * @return
     */
    public static Link acquire() {
        if (link==null){
            Log.e(TAG, "Link has not been initialized");
        }
        return link;
    }

    /**
     * 接口API
     *
     * @return
     */
    public static Alink api() {
        return link.alink;
    }

    /**
     * 物联网api
     *
     * @return
     */
    public Alink getAlink() {
        return alink;
    }

    /**
     * 初始化，默认读取三元组信息，服务器区域：cn-shanghai，端口：1883
     *
     * @param context 上下文
     * @param type    授权类型
     * @return
     */
    public static Link initialize(Context context, LicenseType type) {
        License license = License.acquire();
        license.setType(type);
        String url = ServerURL.value(false, license.getProductKey(), "cn-shanghai", 1883);
        return initialize(context, url, license);
    }

    /**
     * 初始化,默认采用服务器区域：cn-shanghai，端口：1883
     *
     * @param context 上下文
     * @param license 三元组许可
     * @return
     */
    public static Link initialize(Context context, License license) {
        String url = ServerURL.value(false, license.getProductKey(), "cn-shanghai", 1883);
        return initialize(context, url, license);
    }

    /**
     * 初始化
     *
     * @param context 上下文
     * @param url     服务端地址，例如：tcp://a1mFnrTMwKg.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883
     * @param license 授权许可
     * @return
     */
    public static Link initialize(Context context, String url, License license) {
        if (link == null) {
            synchronized (Link.class) {
                if (link == null) {
                    link = new Link(context, url, license);
                }
            }
        }
        return link;
    }

    @Override
    public void onSuccess(IMqttToken token) {
        alink = new Alink(this, license);
        alink.subscribeOTA();
        super.onSuccess(token);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (alink != null) {
            alink.destroy();
        }
        alink = null;
        link = null;
    }

}
