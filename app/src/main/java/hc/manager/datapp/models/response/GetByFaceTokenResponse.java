package hc.manager.datapp.models.response;

import hc.manager.datapp.app.UserItem;

public class GetByFaceTokenResponse extends BaseResponse {
    public UserItem user;

    public GetByFaceTokenResponse(String message, int status) {
        super(message, status);
    }

    public UserItem getUser() {
        return user;
    }

    public void setUser(UserItem user) {
        this.user = user;
    }
}
