package hc.manager.datapp.models.request;

import hc.manager.datapp.utils.Constant;

public class GetDeviceBySeriRequest {
    public String seri;
    public String version;

    public GetDeviceBySeriRequest(String seri) {
        this.version = Constant.VersionDat;
        this.seri = seri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }
}
