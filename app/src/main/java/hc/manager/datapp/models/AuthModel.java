package hc.manager.datapp.models;

public class AuthModel {
    public long Time;
    public String Seri;
    public String UserCode;
    public String Status;
    public String FilePath;
    public double Lat;
    public double Lng;
    public float Dis;
    public String TeacherCode;
    public String SessionId;
    // 1 là thành công, 0 là thất bại
    public int Sent;
    public String FilePathLocal;
    public double Speed;

    public AuthModel(int status) {
        Status = String.valueOf(status);
//        Time = (System.currentTimeMillis() / 1000) + 25000;
        Time = (System.currentTimeMillis() / 1000) + 25190; //7h 25200
    }

    public AuthModel() {

    }

    public String getSessionId() {
        return SessionId;
    }

    public void setSessionId(String sessionId) {
        SessionId = sessionId;
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

    public String getUserCode() {
        return UserCode;
    }

    public void setUserCode(String userCode) {
        UserCode = userCode;
    }

    public long getTime() {
        return Time;
    }

    public void setTime(long time) {
        Time = time;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
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

    public float getDis() {
        return Dis;
    }

    public void setDis(float dis) {
        Dis = dis;
    }

    public double getSpeed() {
        return Speed;
    }

    public void setSpeed(double speed) {
        Speed = speed;
    }

    @Override
    public String toString() {
        return "AuthModel{" +
                "Time=" + Time +
                ", Seri='" + Seri + '\'' +
                ", UserCode='" + UserCode + '\'' +
                ", Status='" + Status + '\'' +
                ", FilePath='" + FilePath + '\'' +
                ", Lat=" + Lat +
                ", Lng=" + Lng +
                ", Dis=" + Dis +
                ", TeacherCode='" + TeacherCode + '\'' +
                ", SessionId='" + SessionId + '\'' +
                ", Sent=" + Sent +
                ", FilePathLocal='" + FilePathLocal + '\'' +
                ", Speed=" + Speed +
                '}';
    }
}
