package hc.manager.datapp.models.request;

public class GetByFaceTokenRequest {
    public String faceToken;
    public String seri;

    public GetByFaceTokenRequest(String _faceToken, String _seri) {
        faceToken = _faceToken;
        seri = _seri;
    }

    public String getFaceToken() {
        return faceToken;
    }

    public void setFaceToken(String faceToken) {
        this.faceToken = faceToken;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }
}
