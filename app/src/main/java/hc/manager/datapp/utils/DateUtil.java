package hc.manager.datapp.utils;

public class DateUtil {
    public static String ConvertHms(double timeD) {
        int hour = (int) Math.floor(timeD / 3600);
        int minute = (int) Math.floor((timeD - (hour * 3600)) / 60);
        double second = timeD - (hour * 3600) - (minute * 60);
        return padLeftZeros(String.valueOf(hour), 2) + " GIỜ " + padLeftZeros(String.valueOf(minute), 2) + " PHÚT";
    }

    public static String padLeftZeros(String inputString, int length) {
        return String.format("%1$" + length + "s", inputString).replace(' ', '0');
    }
}
