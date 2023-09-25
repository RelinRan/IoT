package androidx.iot.aiot;

import android.content.Context;
import android.util.Log;

import androidx.iot.mqtt.Imqtt;
import androidx.iot.mqtt.Mqtt;
import androidx.iot.mqtt.MqttOption;

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
     * 阿里物联网服务api
     */
    private Alink api;

    private Link() {
    }

    /**
     * 设置物联网接口对象
     *
     * @param api
     */
    public void api(Alink api) {
        this.api = api;
    }

    /**
     * 获取物联网接口对象
     *
     * @return
     */
    public Alink api() {
        return api;
    }

    /**
     * 初始化，默认读取三元组信息，服务器区域：cn-shanghai，端口：1883
     *
     * @param context 上下文
     * @return
     */
    public static Link initialize(Context context) {
        License license = License.with(context).load();
        String url = URL(license.getProductKey(), "cn-shanghai", 1883);
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
        String url = URL(license.getProductKey(), "cn-shanghai", 1883);
        return initialize(context, url, license);
    }

    /**
     * 初始化
     *
     * @param context 上下文
     * @param url     服务端地址，例如：tcp://a1mFnrTMwKg.iot-as-mqtt.cn-shanghai.aliyuncs.com:1883
     * @param license 三元组许可
     * @return
     */
    public static Link initialize(Context context, String url, License license) {
        if (link == null) {
            synchronized (Link.class) {
                if (link == null) {
                    link = new Link();
                }
            }
        }
        if (license != null) {
            String productKey = license.getProductKey();
            String deviceName = license.getDeviceName();
            String deviceSecret = license.getDeviceSecret();
            if (productKey != null && deviceName != null && deviceSecret != null) {
                if (link.api() != null) {
                    link.api().setLicense(license);
                } else {
                    link.api(new Alink(link, license));
                }
                LinkOption options = new LinkOption().getLinkOption(productKey, deviceName, deviceSecret);
                if (options == null) {
                    Log.e(TAG, "device info error");
                } else {
                    if (link.isDebug()) {
                        Log.d(TAG, license.toJSONString());
                    }
                    String clientId = options.getClientId();
                    String userName = options.getUsername();
                    String passWord = options.getPassword();
                    link.initializeClient(context, new MqttOption(url, clientId, userName, passWord));
                    Log.i(TAG, "initialize successful");
                }
            } else {
                Log.e(TAG, "initialize failed license parameter incorrect");
            }
        } else {
            Log.e(TAG, "initialize failed license is null");
        }
        return link;
    }

    /**
     * 组合HOST
     *
     * @param productKey 产品key
     * @param area       服务器区域（例如：cn-shanghai）
     * @param port       端口（例如：443）
     * @return
     */
    public static String URL(String productKey, String area, int port) {
        StringBuffer sb = new StringBuffer("tcp://");
        sb.append(productKey);
        sb.append(".iot-as-mqtt.");
        sb.append(area);
        sb.append(".aliyuncs.com:");
        sb.append(port);
        return sb.toString();
    }

    /**
     * 获取阿里物联网MQTT
     *
     * @return
     */
    public static Link mqtt() {
        return link;
    }

    @Override
    public Imqtt reset() {
        if (link != null) {
            link = null;
        }
        return super.reset();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if (api != null) {
            api.release();
        }
    }

}
