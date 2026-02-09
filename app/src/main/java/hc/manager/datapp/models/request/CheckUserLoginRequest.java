package hc.manager.datapp.models.request;

public class CheckUserLoginRequest {
    public String userCode;
    public String seri;

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }
}
