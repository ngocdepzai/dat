package hc.manager.datapp.models;

public class UserModel {
    public byte[] fingerPrintId1;
    public byte[] fingerPrintId2;
    public String id;
    public String username;

    public byte[] getFingerPrintId1() {
        return fingerPrintId1;
    }

    public void setFingerPrintId1(byte[] fingerPrintId1) {
        this.fingerPrintId1 = fingerPrintId1;
    }

    public byte[] getFingerPrintId2() {
        return fingerPrintId2;
    }

    public void setFingerPrintId2(byte[] fingerPrintId2) {
        this.fingerPrintId2 = fingerPrintId2;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
