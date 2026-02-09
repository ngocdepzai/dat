package hc.manager.datapp.models;

public class ResetDeviceModel {
    public String id;
    public String seri;
    public String deviceId;
    public boolean isSuccess;
    public int resetType;
    public String userCode;

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public int getResetType() {
        return resetType;
    }

    public void setResetType(int resetType) {
        this.resetType = resetType;
    }
}
