package hc.manager.datapp.models.request;

public class GetUserByIdRequest {
    public String id;
    public String seri;

    public GetUserByIdRequest(String id, String seri) {
        this.id = id;
        this.seri = seri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

}
