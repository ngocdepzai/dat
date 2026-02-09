package hc.manager.datapp.models.request;

public class GetUpdateUserBySeriRequest {
    public String seri;
    public int type;

    public GetUpdateUserBySeriRequest(String _seri, int _type) {
        seri = _seri;
        type = _type;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
