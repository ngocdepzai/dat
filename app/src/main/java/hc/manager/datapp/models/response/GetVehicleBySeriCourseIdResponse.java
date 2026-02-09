package hc.manager.datapp.models.response;

import java.util.List;

import hc.manager.datapp.app.VehicleItem;

public class GetVehicleBySeriCourseIdResponse extends BaseResponse {

    private List<VehicleItem> vehicles = null;

    public GetVehicleBySeriCourseIdResponse(String message, int status) {
        super(message, status);
    }

    public List<VehicleItem> getItems() {
        return vehicles;
    }
}
