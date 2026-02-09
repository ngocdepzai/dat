package hc.manager.datapp.models;

public class InOutModel {
    public int Type; // 1: là đăng nhập, 2 là đăng xuất
    public String Seri; // Seri thiết bị
    public long Time; // Thời gian lúc người dùng InOut , unixTime
    public String UserCode; // Mã người dùng
    public String UserId; // Id người dùng
    public String Name; // Tên người dùng
    public double Lat;
    public double Lng;
    public int UserType;
    public int LoginType;// 1 : RfId, 2: Finger, 3: Face
    public int Sent;
    public float Dis;
    public String FilePath;
    public String FilePathLocal;

    public InOutModel() {
        Time = (System.currentTimeMillis() / 1000) + 25200;
    }

    public InOutModel(int type) {
        Type = type;
        Time = (System.currentTimeMillis() / 1000) + 25200;
    }

    public int getSent() {
        return Sent;
    }

    public void setSent(int sent) {
        Sent = sent;
    }

    public int getLoginType() {
        return LoginType;
    }

    public void setLoginType(int loginType) {
        LoginType = loginType;
    }

    public float getDis() {
        return Dis;
    }

    public void setDis(float dis) {
        Dis = dis;
    }

    public String getFilePath() {
        return FilePath;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public String getFilePathLocal() {
        return FilePathLocal;
    }

    public void setFilePathLocal(String filePathLocal) {
        FilePathLocal = filePathLocal;
    }

    public long getTime() {
        return Time;
    }

    public void setTime(long time) {
        Time = time;
    }


    public String getSeri() {
        return Seri;
    }

    public void setSeri(String seri) {
        Seri = seri;
    }

    public int getType() {
        return Type;
    }

    public void setType(int type) {
        Type = type;
    }

    public String getUserCode() {
        return UserCode;
    }

    public void setUserCode(String userCode) {
        UserCode = userCode;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public double getLat() {
        return Lat;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLng() {
        return Lng;
    }

    public void setLng(double lng) {
        Lng = lng;
    }
}
