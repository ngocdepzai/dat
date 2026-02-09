package hc.manager.datapp.models.request;

public class GetListSessionRequest {
    public String seri;
    public String startTime;
    public String endTime;
    public int limit;
    public int page;
    public Boolean sendGeneral;

    public GetListSessionRequest() {
        page = 1;
        limit = 12;
        sendGeneral = null;
        startTime = null;
        endTime = null;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Boolean getSendGeneral() {
        return sendGeneral;
    }

    public void setSendGeneral(Boolean sendGeneral) {
        this.sendGeneral = sendGeneral;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
