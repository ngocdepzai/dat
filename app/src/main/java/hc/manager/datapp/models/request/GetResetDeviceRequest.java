package hc.manager.datapp.models.request;

public class GetResetDeviceRequest {
    public String seri;

    public GetResetDeviceRequest(String seri) {
        this.seri = seri;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }
}
