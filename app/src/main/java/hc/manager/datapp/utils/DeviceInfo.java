package hc.manager.datapp.utils;

public class DeviceInfo {
    public String phone_nbr = ""; // 手机号
    public String phone_type; // 手机型号
    public String carrier; // 运营商: CUC, CMC, CTC
    public String carrier_name; // 运营商: CUC, CMC, CTC
    public String net_type; // 网络类型：2G, 3G, 4G

    @Override
    public String toString() {
        return "DeviceInfo [phone_nbr=" + phone_nbr + ", phone_type=" + phone_type + ", carrier="
                + carrier + ", net_type=" + net_type + "]";
    }

}








