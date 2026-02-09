package hc.manager.datapp.models.request;

public class GetRealtimeUserRequest {
    public String userCode;

    public GetRealtimeUserRequest(String userCode) {
        this.userCode = userCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
}
