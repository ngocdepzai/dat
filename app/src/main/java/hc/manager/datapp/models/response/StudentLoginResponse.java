package hc.manager.datapp.models.response;

public class StudentLoginResponse extends BaseResponse {
    public String sessionId;

    public StudentLoginResponse(String message, int status) {
        super(message, status);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
