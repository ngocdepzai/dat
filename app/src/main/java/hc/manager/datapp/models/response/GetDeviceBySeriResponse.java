package hc.manager.datapp.models.response;

import hc.manager.datapp.models.DeviceModel;
import hc.manager.datapp.models.TrainingCenterModel;
import hc.manager.datapp.models.VehicleModel;

public class GetDeviceBySeriResponse extends BaseResponse {
    public DeviceModel device;
    public TrainingCenterModel trainingCenter;
    public VehicleModel vehicle;

    public GetDeviceBySeriResponse(String message, int status) {
        super(message, status);
    }

    public VehicleModel getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleModel vehicle) {
        this.vehicle = vehicle;
    }

    public DeviceModel getDevice() {
        return device;
    }

    public void setDevice(DeviceModel device) {
        this.device = device;
    }

    public TrainingCenterModel getTrainingCenter() {
        return trainingCenter;
    }

    public void setTrainingCenter(TrainingCenterModel trainingCenter) {
        this.trainingCenter = trainingCenter;
    }
}
