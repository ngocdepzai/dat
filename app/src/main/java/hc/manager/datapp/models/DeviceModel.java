package hc.manager.datapp.models;

public class DeviceModel {
    public String id;
    public String seri;
    public String deviceTypeId;
    public String apiUrl;
    public int timeAuth;
    public int timeSendGps;
    public int distanceError;
    public int fakeDisPercent;
    public int warningTime;
    public int warningDistance;
    public Boolean isWarningTime;
    public Boolean isWarningDistance;
    public int fakeTimePercent;

    public int getWarningTime() {
        return warningTime;
    }

    public void setWarningTime(Boolean warningTime) {
        isWarningTime = warningTime;
    }

    public void setWarningTime(int warningTime) {
        this.warningTime = warningTime;
    }

    public int getWarningDistance() {
        return warningDistance;
    }

    public void setWarningDistance(Boolean warningDistance) {
        isWarningDistance = warningDistance;
    }

    public void setWarningDistance(int warningDistance) {
        this.warningDistance = warningDistance;
    }

    public int getFakeDisPercent() {
        return fakeDisPercent;
    }

    public void setFakeDisPercent(int fakeDisPercent) {
        this.fakeDisPercent = fakeDisPercent;
    }

    public int getFakeTimePercent() {
        return fakeTimePercent;
    }

    public void setFakeTimePercent(int fakeTimePercent) {
        this.fakeTimePercent = fakeTimePercent;
    }

    public int getDistanceError() {
        return distanceError;
    }

    public void setDistanceError(int distanceError) {
        this.distanceError = distanceError;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public int getTimeAuth() {
        return timeAuth;
    }

    public void setTimeAuth(int timeAuth) {
        this.timeAuth = timeAuth;
    }

    public int getTimeSendGps() {
        return timeSendGps;
    }

    public void setTimeSendGps(int timeSendGps) {
        this.timeSendGps = timeSendGps;
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

    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }
}
