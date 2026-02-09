package hc.manager.datapp.models;

public class VehicleModel {
    public String id;
    public String plate;
    public String plateSlug;
    public String deviceId;
    public String oldPlace;
    public String deviceGpsId;
    public String deviceCaBinId;
    public String setpointType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getPlateSlug() {
        return plateSlug;
    }

    public void setPlateSlug(String plateSlug) {
        this.plateSlug = plateSlug;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getOldPlace() {
        return oldPlace;
    }

    public void setOldPlace(String oldPlace) {
        this.oldPlace = oldPlace;
    }

    public String getDeviceGpsId() {
        return deviceGpsId;
    }

    public void setDeviceGpsId(String deviceGpsId) {
        this.deviceGpsId = deviceGpsId;
    }

    public String getDeviceCaBinId() {
        return deviceCaBinId;
    }

    public void setDeviceCaBinId(String deviceCaBinId) {
        this.deviceCaBinId = deviceCaBinId;
    }

    public String getSetpointType() {
        return setpointType;
    }

    public void setSetpointType(String setpointType) {
        this.setpointType = setpointType;
    }
}
