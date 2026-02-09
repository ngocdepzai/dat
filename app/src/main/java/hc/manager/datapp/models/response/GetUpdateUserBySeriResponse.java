package hc.manager.datapp.models.response;

import java.util.List;

import hc.manager.datapp.app.UserItem;

public class GetUpdateUserBySeriResponse extends BaseResponse {
    public List<UserItem> users;

    public GetUpdateUserBySeriResponse(String message, int status) {
        super(message, status);
    }

    public List<UserItem> getUsers() {
        return users;
    }

    public void setUsers(List<UserItem> users) {
        this.users = users;
    }
}