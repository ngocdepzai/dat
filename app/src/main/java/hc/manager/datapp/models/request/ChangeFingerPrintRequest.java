package hc.manager.datapp.models.request;

public class ChangeFingerPrintRequest {
    public String code;
    public String seri;
    public String fingerPrintId1;

    public ChangeFingerPrintRequest(String code, String fingerPrintId1, String seri) {
        this.code = code;
        this.fingerPrintId1 = fingerPrintId1;
        this.seri = seri;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

    public String getFingerPrintId1() {
        return fingerPrintId1;
    }

    public void setFingerPrintId1(String fingerPrintId1) {
        this.fingerPrintId1 = fingerPrintId1;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
