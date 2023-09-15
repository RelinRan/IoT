package androidx.iot.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class Data {

    private long size;
    private String sign;
    private String version;
    private String signMethod;
    private String url;
    private String md5;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Data fromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            setSize(obj.optLong("size"));
            setSign(obj.optString("sign"));
            setVersion(obj.optString("version"));
            setSignMethod(obj.optString("signMethod"));
            setUrl(obj.optString("url"));
            setMd5(obj.optString("md5"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

}
