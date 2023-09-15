package androidx.iot.entity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 请求相应内容
 */
public class ResponseBody {

    private long id;
    private int code;
    private String message;
    private Data data;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public ResponseBody fromJson(String json){
        try {
            JSONObject obj = new JSONObject(json);
            setId(obj.optLong("id"));
            setCode(obj.optInt("code"));
            setMessage(obj.optString("id"));
            setData(new Data().fromJson(String.valueOf(obj.get("data"))));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

}
