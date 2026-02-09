package hc.manager.datapp.models.request;

public class DeleteUserDeviceRequest {
    public String userCode;
    public String seri;

    public DeleteUserDeviceRequest(String _userCode, String _seri) {
        userCode = _userCode;
        seri = _seri;
    }

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
