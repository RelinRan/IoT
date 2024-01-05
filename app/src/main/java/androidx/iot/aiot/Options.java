package androidx.iot.aiot;

import android.text.TextUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * MQTT建连选项类，输入设备三元组productKey, deviceName和deviceSecret, 生成Mqtt建连参数clientId，username和password.
 */
class Options {

    /**
     * 签名算法。目前支持hmacmd5、hmacsha1、hmacsha256。
     */
    private final String ALGORITHM = "hmacsha256";
    private String username = "";
    private String password = "";
    private String clientId = "";

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getClientId() {
        return this.clientId;
    }

    /**
     * 一机一密、一型一密预注册认证方式：使用设备证书（ProductKey、DeviceName和DeviceSecret）连接。
     *
     * @param productKey   产品秘钥
     * @param deviceName   设备名称
     * @param deviceSecret 设备机密
     * @return
     */
    public Options connect(String productKey, String deviceName, String deviceSecret) {
        if (isEmpty(productKey) || isEmpty(deviceName) || isEmpty(deviceSecret)) {
            return null;
        }
        try {
            String timestamp = Long.toString(System.currentTimeMillis());
            StringBuilder id = new StringBuilder();
            //客户端ID，可自定义，长度在64个字符内。建议使用设备的MAC地址或SN码，方便您识别区分不同的客户端
            id.append(productKey).append(".").append(deviceName);
            id.append("|timestamp=").append(timestamp);
            id.append(",_v=paho-android-1.0.0");
            id.append(",securemode=2");
            id.append(",signmethod=").append(ALGORITHM);//HmacSHA256
            id.append("|");
            // clientId
            this.clientId = id.toString();
            // userName
            this.username = deviceName + "&" + productKey;
            // password
            StringBuilder content = new StringBuilder();
            content.append("clientId").append(productKey).append(".").append(deviceName);
            content.append("deviceName").append(deviceName);
            content.append("productKey").append(productKey);
            content.append("timestamp").append(timestamp);
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(deviceSecret.getBytes(), ALGORITHM);
            mac.init(secretKeySpec);
            byte[] macRes = mac.doFinal(content.toString().getBytes());
            password = String.format("%064x", new BigInteger(1, macRes));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return this;
    }

    /**
     * 一型一密免预注册认证方式：使用ProductKey、DeviceName、ClientID、DeviceToken连接。
     *
     * @param clientId    客户端id
     * @param productKey  产品key
     * @param deviceName  设备名称
     * @param deviceToken 设备令牌
     * @return
     */
    public Options connwl(String clientId, String productKey, String deviceName, String deviceToken) {
        if (isEmpty(clientId) || isEmpty(productKey) || isEmpty(deviceName) || isEmpty(deviceToken)) {
            return null;
        }
        try {
            String timestamp = Long.toString(System.currentTimeMillis());
            StringBuilder id = new StringBuilder();
            id.append(clientId);//客户端ID，可自定义，长度在64个字符内。建议使用设备的MAC地址或SN码，方便您识别区分不同的客户端
            id.append("|timestamp=").append(timestamp);
            id.append(",_v=paho-android-1.0.0");
            id.append(",securemode=-2");
            id.append(",authType=connwl|");
            this.clientId = id.toString();
            username = deviceName + "&" + productKey;
            password = deviceToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return this;
    }

    /**
     * 一型一密预注册认证方式
     * /ext/register
     * {"deviceSecret":"xxx","productKey":"xxx","deviceName":"xxx"}
     *
     * @param instanceId    实列id
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备名称
     * @return
     */
    public Options register(String instanceId, String productKey, String productSecret, String deviceName) {
        if (isEmpty(productKey) || isEmpty(productSecret) || isEmpty(deviceName)) {
            return null;
        }
        dynamic(2, "register", instanceId, productKey, productSecret, deviceName);
        return this;
    }

    /**
     * 一型一密免预注册认证方式
     * /ext/regnwl
     * {"clientId":"xxx","productKey":"xxx","deviceName":"xxx","deviceToken":"xxx"}
     *
     * @param instanceId    实列id
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备名称
     * @return
     */
    public Options regnwl(String instanceId, String productKey, String productSecret, String deviceName) {
        if (isEmpty(productKey) || isEmpty(productSecret) || isEmpty(deviceName)) {
            return null;
        }
        dynamic(-2, "regnwl", instanceId, productKey, productSecret, deviceName);
        return this;
    }

    /**
     * 动态注册
     *
     * @param securemode    安全模式。
     *                      一型一密预注册认证方式：固定取值为2。
     *                      一型一密免预注册认证方式：固定取值为-2。
     * @param authType      一型一密认证方式，不同类型将返回不同的认证参数：
     *                      register：一型一密预注册认证方式，返回DeviceSecret。
     *                      regnwl：一型一密免预注册认证方式，返回DeviceToken、ClientID。
     * @param instanceId    实例ID
     * @param productKey    产品key
     * @param productSecret 产品secret
     * @param deviceName    设备名称
     * @return
     */
    public Options dynamic(int securemode, String authType, String instanceId, String productKey, String productSecret, String deviceName) {
        if (isEmpty(authType) || isEmpty(productKey) || isEmpty(productSecret) || isEmpty(deviceName)) {
            return null;
        }
        try {
            int random = new Random().nextInt(1000000);
            StringBuilder id = new StringBuilder();
            //客户端ID，可自定义，长度在64个字符内。建议使用设备的MAC地址或SN码，方便您识别区分不同的客户端
            id.append(productKey).append(".").append(deviceName);
            id.append("|securemode=").append(securemode);
            id.append(",authType=").append(authType);
            id.append(",signmethod=").append(ALGORITHM);
            id.append(",random=").append(random);
            //实例ID。请登录物联网平台控制台，在实例概览页面查看。
            if (!TextUtils.isEmpty(instanceId)) {
                id.append(",instanceId=").append(instanceId);
            }
            id.append("|");
            clientId = id.toString();
            username = deviceName + "&" + productKey;
            StringBuilder content = new StringBuilder();
            content.append("deviceName").append(deviceName);
            content.append("productKey").append(productKey);
            content.append("random").append(random);
            password = encrypt(content.toString(), productSecret);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return this;
    }

    /**
     * @param value
     * @return
     */
    private boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    /**
     * 使用HMAC_ALGORITHM加密。
     *
     * @param content 明文
     * @param secret  密钥
     * @return 密文
     */
    private String encrypt(String content, String secret) {
        try {
            byte[] text = content.getBytes(StandardCharsets.UTF_8);
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
            Mac mac = Mac.getInstance(secretKey.getAlgorithm());
            mac.init(secretKey);
            return byte2hex(mac.doFinal(text));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 二进制转十六进制字符串。
     *
     * @param b 二进制数组
     * @return 十六进制字符串
     */
    private String byte2hex(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int n = 0; b != null && n < b.length; n++) {
            String stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                sb.append('0');
            }
            sb.append(stmp);
        }
        return sb.toString().toUpperCase();
    }

}