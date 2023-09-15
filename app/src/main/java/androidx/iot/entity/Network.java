package androidx.iot.entity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 上报网络状态
 */
public class Network {

    private Wifi wifi;
    /**
     * 时间戳。
     * 说明 时间戳可以为空。为空时，控制台上设备网络状态不展示采集时间。
     */
    private long _time = System.currentTimeMillis() / 1000;

    public Wifi getWifi() {
        return wifi;
    }

    public void setWifi(Wifi wifi) {
        this.wifi = wifi;
    }

    public long get_time() {
        return _time;
    }

    public void set_time(long _time) {
        this._time = _time;
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("wifi", wifi.toJSONObject());
            object.put("_time", _time);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return object;
    }

}
