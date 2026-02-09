package hc.manager.datapp.models.response;

public class SessionTotalResponse extends BaseResponse {
    public double totalTime = 0;
    public Float totalDis = 0f;

    public SessionTotalResponse(String message, int status) {
        super(message, status);
    }

    public double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    public Float getTotalDis() {
        return totalDis;
    }

    public void setTotalDis(Float totalDis) {
        this.totalDis = totalDis;
    }
}
