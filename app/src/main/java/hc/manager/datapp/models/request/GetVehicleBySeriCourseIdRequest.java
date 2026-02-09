package hc.manager.datapp.models.request;

public class GetVehicleBySeriCourseIdRequest {
    public String deviceSeri;
    public String courseId;

    public GetVehicleBySeriCourseIdRequest(String deviceSeri, String courseId) {
        this.deviceSeri = deviceSeri;
        this.courseId = courseId;
    }

    public String getDeviceSeri() {
        return deviceSeri;
    }

    public void setDeviceSeri(String id) {
        this.deviceSeri = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String id) {
        this.courseId = id;
    }
}
