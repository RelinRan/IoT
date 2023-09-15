package androidx.iot.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class Wifi {

    /**
     * 无线信号接收强度。
     */
    private String rssi;
    /**
     * 无线信号信噪比。
     */
    private String snr;
    /**
     * 数据丢包率。
     */
    private String per;
    /**
     * 错误信息。仅当设备检测到网络异常后，上报数据包含该参数。
     * 格式："type,code,count;type,code,count"，如"10,02,01;10,05,01"。
     *
     * 参数说明：
     *
     * type：错误类型
     * code：错误原因
     * count：错误数量
     * =================[err_stats]=================
     * 0x00	无线环境参数。
     * 信号强度（RSSI）：0x01
     * 信噪比（SNR）：0x02
     * 丢包率（drop ratio）：0x03
     *
     * 0x10	设备与云端建立连接失败。
     * 路由器连接失败（Wi-Fi fail）：0x01
     * DHCP失败，获取IP地址失败（DHCP fail）：0x02
     * DNS失败，解析云端的域名失败（DNS fail）：0x03
     * TCP握手失败（TCP fail）：0x04
     * TLS握手失败（TLS fail）：0x05
     *
     * 0x20	设备与云端的网络异常
     * 云端主动断开与设备的连接（CLOUD_REJECT）：0x01
     * 设备数据上下行失败（RW_EXCEPTION）：0x02
     * 设备与云端的PING操作异常（PING_EXCEPTION）：0x03
     *
     * 0x30	设备运行异常。
     * 看门狗复位重启（WD_RST）： 0x01
     * 设备存储异常重启（PANIC_ERR）：0x02
     * 设备掉电上电重启（RE-POWER）：0x03
     * 设备运行异常重启（FATAL_ERR）：0x04
     *
     * 0x40	设备内存动态监控。
     * 内存总量（type of total size）：0x01
     * 空闲内存总量（type of free size）：0x02
     *
     * 0x50	BLE异常。	BLE异常
     */
    private String err_stats;

    public String getRssi() {
        return rssi;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    public String getSnr() {
        return snr;
    }

    public void setSnr(String snr) {
        this.snr = snr;
    }

    public String getPer() {
        return per;
    }

    public void setPer(String per) {
        this.per = per;
    }

    public String getErr_stats() {
        return err_stats;
    }

    public void setErr_stats(String err_stats) {
        this.err_stats = err_stats;
    }

    public JSONObject toJSONObject(){
        JSONObject object = new JSONObject();
        try {
            object.put("rssi",rssi);
            object.put("snr",snr);
            object.put("per",per);
            object.put("err_stats",err_stats);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return object;
    }

}
