package hc.manager.datapp.models.request;

public class AddFaceTokenRequest {
    public String userCode;
    public String seri;
    public String faceToken;

    public AddFaceTokenRequest(String _userCode, String _seri, String _faceToken) {
        this.userCode = _userCode;
        this.seri = _seri;
        this.faceToken = _faceToken;
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

    public String getFaceToken() {
        return faceToken;
    }

    public void setFaceToken(String faceToken) {
        this.faceToken = faceToken;
    }
}
