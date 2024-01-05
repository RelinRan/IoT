package androidx.iot.aiot;

import android.content.Context;
import android.text.TextUtils;

import androidx.iot.dialog.OTADialog;
import androidx.iot.entity.Log;
import androidx.iot.entity.Network;
import androidx.iot.entity.ResponseBody;
import androidx.iot.log.LogLevel;
import androidx.iot.mqtt.Imqtt;
import androidx.iot.utils.Apk;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 阿里物联网 - Alink协议接口
 */
public class Alink {

    private String TAG = Alink.class.getSimpleName();
    /**
     * 三元组许可
     */
    private License license;
    /**
     * MQTT操作对象
     */
    private Imqtt mqtt;
    /**
     * 更新对话框
     */
    private OTADialog otaDialog;

    /**
     * 构建物联网API
     *
     * @param mqtt    MQTT操作对象
     * @param license 三元组信息
     */
    public Alink(Imqtt mqtt, License license) {
        this.mqtt = mqtt;
        this.license = license;
    }

    /**
     * 设置三元组信息
     */
    public void setLicense(License license) {
        this.license = license;
    }

    /**
     * 获取三元组
     *
     * @return
     */
    public License getLicense() {
        return license;
    }

    //************************************[公共方法]*****************************************

    /**
     * 是否是对应的主题
     *
     * @param topic 枚举主题
     * @param value 主题值
     * @return
     */
    public boolean isTopic(Topic topic, String value) {
        return topic.equals(value);
    }

    /**
     * 是否是对应的主题
     *
     * @param topic           枚举主题
     * @param value           主题值
     * @param functionBlockId 物模型模块id
     * @param identifier      属性标识符
     * @return
     */
    public boolean isTopic(Topic topic, String value, String functionBlockId, String identifier) {
        return topic.equals(value, functionBlockId, identifier);
    }

    /**
     * 订阅
     *
     * @param topic 主题
     */
    public void subscribe(String topic) {
        if (mqtt == null || !mqtt.isConnected()) {
            System.err.println("MQTT did not connect successfully");
            return;
        }
        mqtt.subscribe(topic);
    }

    /**
     * 订阅
     *
     * @param topic 主题
     */
    public void subscribe(Topic topic) {
        subscribe(topic.real(license.getProductKey(), license.getDeviceName()));
    }

    /**
     * 订阅
     *
     * @param topic           枚举主题
     * @param functionBlockId 物模型模块id
     * @param identifier      属性标识符
     */
    public void subscribe(Topic topic, String functionBlockId, String identifier) {
        subscribe(topic.real(license.getProductKey(), license.getDeviceName(), functionBlockId, identifier));
    }

    /**
     * 发布
     *
     * @param topic   主题
     * @param payload 内容
     */
    public void publish(String topic, String payload) {
        mqtt.publish(topic, payload);
    }

    /**
     * 发布
     *
     * @param topic   主题
     * @param payload 内容
     */
    public void publish(Topic topic, String payload) {
        if (mqtt == null || !mqtt.isConnected()) {
            System.err.println("MQTT did not connect successfully");
            return;
        }
        publish(topic.real(license.getProductKey(), license.getDeviceName()), payload);
    }

    /**
     * 发布
     *
     * @param topic           枚举主题
     * @param functionBlockId 物模型模块id
     * @param identifier      属性标识符
     * @param payload         内容
     */
    public void publish(Topic topic, String functionBlockId, String identifier, String payload) {
        publish(topic.real(license.getProductKey(), license.getDeviceName(), functionBlockId, identifier), payload);
    }

    /**
     * 无方法的JSONObject
     *
     * @return
     */
    public JSONObject getJSONObject() {
        return getJSONObject(null);
    }

    /**
     * 通过方法获取JSONObject
     *
     * @param method 方法名称
     * @return
     */
    public JSONObject getJSONObject(String method) {
        JSONObject object = new JSONObject();
        JSONObjectPut(object, "id", System.currentTimeMillis() + "");
        JSONObjectPut(object, "version", "1.0");
        if (!TextUtils.isEmpty(method)) {
            JSONObjectPut(object, "method", method);
        }
        return object;
    }

    /**
     * JSONObject插入值
     *
     * @param object JSONObject
     * @param key    键
     * @param value  值
     */
    public void JSONObjectPut(JSONObject object, String key, Object value) {
        if (value != null) {
            try {
                object.putOpt(key, value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * JSONObject
     *
     * @param json JSON
     * @return
     */
    public JSONObject newJSONObject(String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * sys下的扩展功能字段
     *
     * @param ack 1：云端返回响应数据。0：云端不返回响应数据。
     * @return
     */
    public JSONObject getSys(int ack) {
        JSONObject sys = new JSONObject();
        try {
            sys.put("ack", ack);
            return sys;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //***************************************[END]*****************************************

    /**
     * 订阅OTA升级主题
     */
    public void subscribeOTA() {
        //物联网平台推送OTA升级包信息
        subscribe(Topic.SUB_OTA_UPGRADE);
        subscribe(Topic.SUB_OTA_FIRMWARE_GET);
    }

    /**
     * 设备请求OTA升级包信息
     *
     * @param module 升级包所属的模块名,不指定则表示请求默认（default）模块的升级包信息。
     */
    public void publishOTAGet(String module) {
        JSONObject object = getJSONObject("thing.ota.firmware.get");
        JSONObject params = new JSONObject();
        module = TextUtils.isEmpty(module) ? "default" : module;
        JSONObjectPut(params, "module", module);
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_OTA_FIRMWARE_GET, object.toString());
    }

    /**
     * 是否需要OTA升级
     *
     * @param context 上下文
     * @param message MQTT消息
     * @return
     */
    public boolean isOTAUpgrade(Context context, MqttMessage message) {
        String payload = new String(message.getPayload());
        ResponseBody response = new ResponseBody().fromJson(payload);
        if (response.getData() == null) {
            return false;
        }
        String version = response.getData().getVersion();
        return Apk.isNewVersion(context, version);
    }

    /**
     * 显示升级dialog
     *
     * @param context 页面
     * @param cancel  是否显示取消按钮
     * @param message MQTT消息
     */
    public void showOTADialog(Context context, boolean cancel, MqttMessage message) {
        String payload = new String(message.getPayload());
        ResponseBody response = new ResponseBody().fromJson(payload);
        if (response.getData() == null) {
            return;
        }
        String version = response.getData().getVersion();
        String filename = Apk.getApplicationName(context) + "_" + version + ".apk";
        String url = response.getData().getUrl();
        boolean isUpgrade = Apk.isNewVersion(context, version);
        if (isUpgrade) {
            if (otaDialog != null) {
                otaDialog.dismiss();
                otaDialog = null;
            }
            otaDialog = new OTADialog(context);
            otaDialog.setAliot(true);
            otaDialog.setCancel(cancel);
            if (!otaDialog.isShowing()) {
                otaDialog.setSource(url, filename);
                otaDialog.show();
            }
        } else {
            android.util.Log.i(TAG, "The current version is the latest version");
        }
    }

    /**
     * 获取OTA Dialog
     *
     * @return
     */
    public OTADialog getOTADialog() {
        return otaDialog;
    }

    /**
     * 消失OTA升级Dialog
     */
    public void dismissOTADialog() {
        if (otaDialog != null) {
            otaDialog.dismiss();
        }
    }

    /**
     * 设备上报OTA模块版本
     *
     * @param module  模块名
     * @param version 模块版本
     */
    public void publishOTAVersion(String module, String version) {
        JSONObject object = new JSONObject();
        JSONObjectPut(object, "id", System.currentTimeMillis() + "");
        JSONObject params = new JSONObject();
        JSONObjectPut(params, "version", version);
        module = TextUtils.isEmpty(module) ? "default" : module;
        JSONObjectPut(params, "module", module);
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_OTA_INFORM, object.toString());
    }

    /**
     * 设备上报升级进度
     *
     * @param step   OTA升级进度。
     *               <p>
     *               取值范围：
     *               1~100的整数：升级进度百分比。
     *               -1：升级失败。
     *               -2：下载失败。
     *               -3：校验失败。
     *               -4：烧写失败。
     * @param desc   当前步骤的描述信息，长度不超过128个字符。如果发生异常，此字段可承载错误信息。
     * @param module 升级包所属的模块名。模块的更多信息
     */
    public void publishProgress(int step, String desc, String module) {
        JSONObject object = new JSONObject();
        JSONObjectPut(object, "id", System.currentTimeMillis() + "");
        JSONObject params = new JSONObject();
        JSONObjectPut(params, "step", step);
        JSONObjectPut(params, "desc", desc);
        JSONObjectPut(params, "module", module);
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_OTA_PROGRESS, object.toString());
    }

    /**
     * 设备上报属性
     *
     * @param property 自定义字段属性名
     */
    public void publishProperty(JSONObject property) {
        subscribe(Topic.SUB_PROPERTY);
        JSONObject object = getJSONObject("thing.event.property.post");
        JSONObjectPut(object, "sys", getSys(0));
        JSONObjectPut(object, "params", property);
        publish(Topic.PUB_PROPERTY, object.toString());
    }

    /**
     * 设备上报属性
     *
     * @param json 自定义字段属性JSON
     */
    public void publishProperty(String json) {
        publishProperty(newJSONObject(json));
    }

    /**
     * 设备上报属性
     *
     * @param property 自定义字段属性名
     */
    public void publishProperty(Map<String, Object> property) {
        publishProperty(new JSONObject(property));
    }

    /**
     * 设备上报日志内容
     *
     * @param logs
     */
    public void publishLog(List<Log> logs) {
        subscribe(Topic.SUB_LOG);
        JSONObject object = getJSONObject("thing.log.post");
        JSONObjectPut(object, "sys", getSys(0));
        JSONArray params = new JSONArray();
        for (Log item : logs) {
            params.put(item.toJSONObject());
        }
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_LOG, object.toString());
    }

    /**
     * 设备上报日志内容
     *
     * @param type    日志级别
     * @param module  模块名称
     * @param code    结果状态码
     * @param traceId 追踪ID
     * @param content 日志内容详情
     */
    public void publishLog(LogLevel type, String module, String code, String traceId, String content) {
        subscribe(Topic.SUB_LOG);
        JSONObject object = getJSONObject("thing.log.post");
        JSONObjectPut(object, "sys", getSys(0));
        JSONArray params = new JSONArray();
        JSONObject item = new JSONObject();
        JSONObjectPut(item, "utcTime", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()));
        JSONObjectPut(item, "logLevel", type.getLevel());
        JSONObjectPut(item, "module", module);
        JSONObjectPut(item, "code", code);
        JSONObjectPut(item, "traceContext", traceId);
        JSONObjectPut(item, "logContent", content);
        params.put(item);
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_LOG, object.toString());
    }

    /**
     * 上传网络状态
     *
     * @param network
     */
    public void publishNetwork(Network network) {
        subscribe(Topic.SUB_NETWORK);
        JSONObject object = getJSONObject();
        JSONObject params = new JSONObject();
        JSONObjectPut(params, "p", network.toJSONObject());
        JSONObjectPut(params, "model", "quantity=single|format=simple|time=now");
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_NETWORK, object.toString());
    }

    /**
     * 上传网络状态（历史数据：非立即上报的数据。设备在日常诊断中，采集到网络正常的指标数据可以延迟上报。设备可以批量上报历史数据）
     *
     * @param list
     */
    public void publishNetwork(List<Network> list) {
        subscribe(Topic.SUB_NETWORK);
        JSONObject object = getJSONObject();
        JSONObject params = new JSONObject();
        JSONArray array = new JSONArray();
        for (Network item : list) {
            array.put(item.toJSONObject());
        }
        JSONObjectPut(params, "p", array);
        JSONObjectPut(params, "model", "format=simple|quantity=batch|time=history");
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_NETWORK, object.toString());
    }

    /**
     * 订阅事件
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     */
    public void subscribeEvent(String functionBlockId, String identifier) {
        subscribe(Topic.SUB_EVENT, functionBlockId, identifier);
    }

    /**
     * 事件上报
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     * @param value           事件数据
     */
    public void publishEvent(String functionBlockId, String identifier, JSONObject value) {
        JSONObject object = getJSONObject("thing.event." + identifier + ".post");
        JSONObjectPut(object, "sys", getSys(0));
        JSONObject params = new JSONObject();
        JSONObjectPut(params, "time", System.currentTimeMillis() + "");
        JSONObjectPut(params, "value", value);
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_EVENT, functionBlockId, identifier, object.toString());
    }

    /**
     * 事件上报
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     * @param json            事件数据
     */
    public void publishEvent(String functionBlockId, String identifier, String json) {
        publishEvent(functionBlockId, identifier, newJSONObject(json));
    }

    /**
     * 事件上报
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     * @param params          事件数据
     */
    public void publishEvent(String functionBlockId, String identifier, Map<String, Object> params) {
        publishEvent(functionBlockId, identifier, new JSONObject(params));
    }

    /**
     * 订阅服务调用（异步调用）
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     */
    public void subscribeService(String functionBlockId, String identifier) {
        subscribe(Topic.SUB_SERVICE, functionBlockId, identifier);
    }

    /**
     * 发布服务调用（异步调用）
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     * @param params          服务参数
     */
    public void publishService(String functionBlockId, String identifier, JSONObject params) {
        JSONObject object = getJSONObject("thing.service." + identifier);
        JSONObjectPut(object, "params", params);
        publish(Topic.PUB_SERVICE, functionBlockId, identifier, object.toString());
    }

    /**
     * 发布服务调用（异步调用）
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     * @param json            服务参数JSON
     */
    public void publishService(String functionBlockId, String identifier, String json) {
        publishService(functionBlockId, identifier, newJSONObject(json));
    }

    /**
     * 发布服务调用（异步调用）
     *
     * @param functionBlockId 自定义模块id,默认模块为空.
     * @param identifier      属性标识符
     * @param params          服务参数JSON
     */
    public void publishService(String functionBlockId, String identifier, Map<String, Object> params) {
        publishService(functionBlockId, identifier, new JSONObject(params));
    }

    /**
     * 设备请求远程通道认证信息
     */
    public void publishTunnelProxy() {
        publish(Topic.PUB_TUNNEL_PROXY, getJSONObject().toString());
    }

    /**
     * 释放资源
     */
    public void destroy() {
        if (otaDialog != null) {
            otaDialog.dismiss();
            otaDialog = null;
        }
    }

}
