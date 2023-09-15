package androidx.iot.mqtt;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Mqtt参数
 */
public class MqttOption {

    /**
     * 服务器地址
     */
    private String host;
    /**
     * 客户端id
     */
    private String clientId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 密码
     */
    private String password;

    public MqttOption() {
    }

    public MqttOption(String host, String clientId, String userName, String password) {
        this.host = host;
        this.clientId = clientId;
        this.userName = userName;
        this.password = password;
    }

    /**
     * JSON解析到参数
     *
     * @param json JSONObject数据
     * @return
     */
    public static MqttOption from(String json) {
        MqttOption options = new MqttOption();
        try {
            JSONObject object = new JSONObject(json);
            options.setHost(object.optString("host"));
            options.setClientId(object.optString("clientId"));
            options.setUserName(object.optString("userName"));
            options.setPassword(object.optString("password"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return options;
    }

    /**
     * 转JSONObject字符串
     *
     * @return
     */
    public String toJSONString() {
        JSONObject object = new JSONObject();
        try {
            object.put("host", host);
            object.put("clientId", clientId);
            object.put("userName", userName);
            object.put("password", password);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return object.toString();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
