package hc.manager.datapp.models;

public class GpsModel {
    public long Time; // Thời gian lúc người dùng InOut , unixTime
    public String Status;
    public String UserCode; // Mã người dùng
    public String Dir; // Tên người dùng
    public float Dis; // Tên người dùng
    public double Lat;
    public double Lng;
    public double Vel;
    public int GpsStatus;
    public int GsmStatus;
    public String Seri; // Seri thiết bị
    public int Sent;
    public String TeacherCode;
    public String SessionId;

    public GpsModel() {
        Time = (System.currentTimeMillis() / 1000) + 25200;
        Status = "0001";
        Vel = 0;
    }

    public String getSessionId() {
        return SessionId;
    }

    public void setSessionId(String sessionId) {
        SessionId = sessionId;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public float getDis() {
        return Dis;
    }

    public void setDis(float dis) {
        Dis = dis;
    }

    public String getTeacherCode() {
        return TeacherCode;
    }

    public void setTeacherCode(String teacherCode) {
        TeacherCode = teacherCode;
    }

    public int getSent() {
        return Sent;
    }

    public void setSent(int sent) {
        Sent = sent;
    }

    public String getSeri() {
        return Seri;
    }

    public void setSeri(String seri) {
        Seri = seri;
    }

    public long getTime() {
        return Time;
    }

    public void setTime(long time) {
        Time = time;
    }

    public String getUserCode() {
        return UserCode;
    }

    public void setUserCode(String userCode) {
        UserCode = userCode;
    }

    public String getDir() {
        return Dir;
    }

    public void setDir(String dir) {
        Dir = dir;
    }

    public double getLat() {
        return Lat;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getVel() {
        return Vel;
    }

    public void setVel(double vel) {
        Vel = vel;
    }

    public double getLng() {
        return Lng;
    }

    public void setLng(double lng) {
        Lng = lng;
    }

    public int getGpsStatus() {
        return GpsStatus;
    }

    public void setGpsStatus(int gpsStatus) {
        GpsStatus = gpsStatus;
    }

    public int getGsmStatus() {
        return GsmStatus;
    }

    public void setGsmStatus(int gsmStatus) {
        GsmStatus = gsmStatus;
    }

    @Override
    public String toString() {
        return "GpsModel{" +
                "Time=" + Time +
                ", Status='" + Status + '\'' +
                ", UserCode='" + UserCode + '\'' +
                ", Dir='" + Dir + '\'' +
                ", Dis=" + Dis +
                ", Lat=" + Lat +
                ", Lng=" + Lng +
                ", Vel=" + Vel +
                ", GpsStatus=" + GpsStatus +
                ", GsmStatus=" + GsmStatus +
                ", Seri='" + Seri + '\'' +
                ", Sent=" + Sent +
                ", TeacherCode='" + TeacherCode + '\'' +
                ", SessionId='" + SessionId + '\'' +
                '}';
    }
}
