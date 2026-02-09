package hc.manager.datapp.models.request;

public class DeleteResetDeviceRequest {
    public String seri;
    public int resetType;

    public DeleteResetDeviceRequest(String _seri, int _resetType) {
        this.seri = _seri;
        this.resetType = _resetType;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

    public int getResetType() {
        return resetType;
    }

    public void setResetType(int resetType) {
        this.resetType = resetType;
    }
}
