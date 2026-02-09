package hc.manager.datapp.models.response;

import java.util.List;

import hc.manager.datapp.models.SessionModel;

public class GetListSessionResponse extends BaseResponse {
    public int total;
    public List<SessionModel> sessions;

    public GetListSessionResponse(String message, int status) {
        super(message, status);
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<SessionModel> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionModel> sessions) {
        this.sessions = sessions;
    }
}
