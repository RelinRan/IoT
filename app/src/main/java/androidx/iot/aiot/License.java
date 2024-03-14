package androidx.iot.aiot;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.iot.receiver.LicenseReceiver;
import androidx.iot.text.Reader;
import androidx.iot.text.Writer;
import androidx.iot.utils.AES;
import androidx.iot.utils.Device;
import androidx.iot.utils.External;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * 阿里物联网 - 授权认证信息
 */
public class License {

    private final static String TAG = License.class.getSimpleName();
    /**
     * 项目文件夹
     */
    private String project;
    /**
     * 三元组文件夹
     */
    private String dir;
    /**
     * 三元组文件名称
     */
    private String name;
    /**
     * 字段名 - clientId(一型一密免预注册认证方式)
     */
    private String clientIdField = "clientId";
    /**
     * 字段名 - deviceToken(一型一密免预注册认证方式)
     */
    private String deviceTokenField = "deviceToken";
    /**
     * 字段名 - deviceName
     */
    private String deviceNameField = "deviceName";
    /**
     * 字段名 - productKey
     */
    private String productKeyField = "productKey";
    /**
     * 字段名 - deviceSecret
     */
    private String deviceSecretField = "deviceSecret";
    /**
     * 客户端id(一型一密免预注册认证方式)
     */
    private String clientId;
    /**
     * 设备令牌(一型一密免预注册认证方式)
     */
    private String deviceToken;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 产品Key
     */
    private String productKey;
    /**
     * 设备密钥
     */
    private String deviceSecret;
    /**
     * U盘接收器
     */
    private LicenseReceiver receiver;
    /**
     * 三元组对象
     */
    private static License license;
    /**
     * 是否已注册
     */
    private boolean register;
    /**
     * 上下文
     */
    private Context context;
    /**
     * 授权类型
     */
    private LicenseType type;

    /**
     * 获取上下文
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * 获取执行对象
     *
     * @return
     */
    public static License acquire() {
        if (license == null) {
            new RuntimeException("License not initialized yet.").printStackTrace();
            return null;
        }
        return license;
    }

    /**
     * 初始化
     *
     * @param type    授权类型
     * @param context 上下文
     * @param project 项目名称
     * @param dir     文件夹
     * @param name    文件名称
     * @return
     */
    public static License initialize(LicenseType type, Context context, String project, String dir, String name) {
        if (license == null) {
            synchronized (License.class) {
                if (license == null) {
                    license = new License(type, context, project, dir, name);
                }
            }
        }
        return license;
    }

    /**
     * 获取授权类型
     * @return
     */
    public LicenseType getType() {
        return type;
    }

    /**
     * 设置授权类型
     * @param type
     */
    public void setType(LicenseType type) {
        this.type = type;
    }

    /**
     * 构造函数
     *
     * @param type    授权类型
     * @param context 上下文
     * @param project 项目名称
     * @param dir     三元组文件夹
     * @param name    三元组文件
     */
    private License(LicenseType type, Context context, String project, String dir, String name) {
        this.context = context;
        this.type = type;
        this.project = project;
        this.dir = dir;
        this.name = name;
        load();
        Log.i(TAG, "initialize project = " + project + ",dir = " + dir + ",name = " + name);
    }

    /**
     * 文件内容字段名称
     *
     * @param deviceName   DEVICE_NAME - 字段
     * @param productKey   PRODUCT_KEY - 字段
     * @param deviceSecret DEVICE_SECRET - 字段
     */
    public void setFields(String deviceName, String productKey, String deviceSecret) {
        deviceNameField = deviceName;
        productKeyField = productKey;
        deviceSecretField = deviceSecret;
        Log.d(TAG, "set fields deviceName = " + deviceName + ",productKey = " + productKey + ",deviceSecret = " + deviceSecret);
    }

    /**
     * 文件内容字段名称
     *
     * @param clientId    clientId - 字段
     * @param deviceToken deviceToken - 字段
     * @param deviceName  deviceName - 字段
     * @param productKey  productKey - 字段
     */
    public void setFields(String clientId, String deviceName, String productKey, String deviceToken) {
        clientIdField = clientId;
        deviceNameField = deviceName;
        productKeyField = productKey;
        deviceTokenField = deviceToken;
        Log.d(TAG, "set fields clientId = " + clientId + ",deviceToken = " + deviceToken + ",deviceName = " + deviceName + ",productKey = " + productKey);
    }

    /**
     * 文件内容字段名称
     *
     * @param clientId     clientId - 字段
     * @param deviceToken  deviceToken - 字段
     * @param deviceName   deviceName - 字段
     * @param productKey   productKey - 字段
     * @param deviceSecret deviceSecret - 字段
     */
    public void setFields(String clientId, String deviceToken, String deviceName, String productKey, String deviceSecret) {
        clientIdField = clientId;
        deviceTokenField = deviceToken;
        deviceNameField = deviceName;
        productKeyField = productKey;
        deviceSecretField = deviceSecret;
        Log.d(TAG, "set fields clientId = " + clientId + ",deviceToken = " + deviceToken + ",deviceName = " + deviceName + ",productKey = " + productKey + ",deviceSecret = " + deviceSecret);
    }

    /**
     * 添加三元组监听
     *
     * @param listener
     */
    public void register(OnLicenseListener listener) {
        if (receiver == null) {
            receiver = new LicenseReceiver();
            receiver.register(context);
            register = true;
        }
        receiver.addTriplesListener(listener);
    }

    /**
     * 移除三元组监听
     */
    public void unregister() {
        if (receiver != null && register) {
            receiver.unregister(context);
            receiver = null;
            register = false;
        }
    }

    /**
     * 是否是JSONObject
     *
     * @param json
     * @return
     */
    public boolean isJSONObject(String json) {
        if (TextUtils.isEmpty(json)) {
            return false;
        }
        try {
            new JSONObject(json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 是否是JSONArray
     *
     * @param json
     * @return
     */
    public boolean isJSONArray(String json) {
        if (TextUtils.isEmpty(json)) {
            return false;
        }
        try {
            new JSONArray(json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 从内容JSON获取当前设备需要的三元组
     *
     * @param json
     * @return
     */
    public License fromJSON(String json) {
        if (isJSONObject(json)) {
            return fromJSONObject(json);
        }
        if (isJSONArray(json)) {
            return fromJSONArray(json);
        }
        return this;
    }

    /**
     * 从JSON获取三元组对象
     *
     * @param content 三元组信息
     * @return
     */
    private License fromJSONObject(String content) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(content);
            setClientId(object.optString(clientIdField));
            setDeviceToken(object.optString(deviceTokenField));
            setDeviceName(object.optString(deviceNameField));
            setProductKey(object.optString(productKeyField));
            setDeviceSecret(object.optString(deviceSecretField));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * 从JSON获取三元组对象
     *
     * @param content 三元组信息
     * @return
     */
    private License fromJSONArray(String content) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        try {
            String deviceId = Device.getUniqueId(context);
            JSONArray jsonArray = new JSONArray(content);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = (JSONObject) jsonArray.get(i);
                String deviceName = obj.optString(deviceNameField);
                if (deviceId.equals(deviceName)) {
                    setDeviceName(deviceName);
                    setClientId(obj.optString(clientIdField));
                    setDeviceToken(obj.optString(deviceTokenField));
                    setProductKey(obj.optString(productKeyField));
                    setDeviceSecret(obj.optString(deviceSecretField));
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * 是否许可过
     *
     * @return
     */
    public boolean isLicensed() {
        String content = getLicense();
        if (TextUtils.isEmpty(content)) {
            return false;
        }
        return true;
    }

    /**
     * 加载三元组
     *
     * @return
     */
    public License load() {
        String content = getLicense();
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        return fromJSON(content);
    }

    /**
     * 获取项目名
     *
     * @return
     */
    public String getProject() {
        return project;
    }

    /**
     * 设置项目名
     *
     * @param project
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * 获取文件夹
     *
     * @return
     */
    public String getDir() {
        return dir;
    }

    /**
     * 设置文件夹
     *
     * @param dir
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * 获取三元组文件
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 设置三元组文件
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取设备名称
     *
     * @return
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * 设置设备名称
     *
     * @param deviceName
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * 获取产品密钥
     *
     * @return
     */
    public String getProductKey() {
        return productKey;
    }

    /**
     * 设置产品密钥
     *
     * @param productKey
     */
    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    /**
     * 获取设备密钥
     *
     * @return
     */
    public String getDeviceSecret() {
        return deviceSecret;
    }

    /**
     * 设置设备密钥
     *
     * @param deviceSecret
     */
    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    /**
     * 客户端id
     * @return
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置客户端id
     * @param clientId
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * 设备令牌
     * @return
     */
    public String getDeviceToken() {
        return deviceToken;
    }

    /**
     * 设置设备令牌
     * @param deviceToken
     */
    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    /**
     * 获取三元组文件夹
     *
     * @return
     */
    public File getLicenseDir() {
        return External.getStorageDir(context, project, dir);
    }

    /**
     * 获取三元组文件
     *
     * @return
     */
    public File getLicenseKey() {
        return new File(getLicenseDir(), "license.key");
    }

    /**
     * 获取三元组文件
     *
     * @return
     */
    public File getLicenseIni() {
        return new File(getLicenseDir(), "license.ini");
    }

    /**
     * 授权三元组信息到文件
     */
    public void granted() {
        String content = toJSONString();
        //密钥文件
        File keyFile = getLicenseKey();
        String key = AES.randomKey();
        Writer keyWriter = new Writer(keyFile);
        keyWriter.sync(key, false);
        //许可文件
        File iniFile = getLicenseIni();
        String ini = AES.encrypt(content, key);
        Writer iniWriter = new Writer(iniFile);
        iniWriter.sync(ini, false);
    }

    /**
     * 读取三元组许可信息
     *
     * @return
     */
    public String getLicense() {
        File licenseKey = getLicenseKey();
        if (!licenseKey.exists()) {
            return null;
        }
        Reader keyReader = new Reader(licenseKey);
        String key = keyReader.sync();
        keyReader.cancel();
        if (key == null) {
            return null;
        }
        key = key.replace("\n", "");
        File licenseIni = getLicenseIni();
        if (!licenseIni.exists()) {
            return null;
        }
        Reader iniReader = new Reader(licenseIni);
        String ini = iniReader.sync();
        iniReader.cancel();
        if (ini == null) {
            return null;
        }
        ini = ini.replace("\n", "");
        String content = AES.decrypt(ini, key);
        return content;
    }

    /**
     * 注销三元组信息
     */
    public void revoked() {
        getLicenseKey().delete();
        getLicenseIni().delete();
    }

    /**
     * 转JSON字符串
     *
     * @return
     */
    public String toJSONString() {
        JSONObject object = new JSONObject();
        try {
            object.put(clientIdField, clientId == null ? "" : clientId);
            object.put(deviceTokenField, deviceToken == null ? "" : deviceToken);
            object.put(productKeyField, productKey == null ? "" : productKey);
            object.put(deviceNameField, deviceName == null ? "" : deviceName);
            object.put(deviceSecretField, deviceSecret == null ? "" : deviceSecret);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return object.toString();
    }

}
