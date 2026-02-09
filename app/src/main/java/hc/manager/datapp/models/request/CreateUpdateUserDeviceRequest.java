package hc.manager.datapp.models.request;

import java.util.List;

public class CreateUpdateUserDeviceRequest {
    public String seri;
    public List<String> updateUserIds;

    public CreateUpdateUserDeviceRequest(String seri, List<String> updateUserIds) {
        this.seri = seri;
        this.updateUserIds = updateUserIds;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }

    public List<String> getUpdateUserIds() {
        return updateUserIds;
    }

    public void setUpdateUserIds(List<String> updateUserIds) {
        this.updateUserIds = updateUserIds;
    }
}
