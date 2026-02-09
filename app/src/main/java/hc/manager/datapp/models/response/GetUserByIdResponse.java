package hc.manager.datapp.models.response;

import hc.manager.datapp.app.UserItem;

public class GetUserByIdResponse extends BaseResponse {
    public Boolean isStudent;
    public UserItem user;

    public GetUserByIdResponse(String message, int status) {
        super(message, status);
    }

    public Boolean getIsStudent() {
        return isStudent;
    }

    public void setStudent(Boolean student) {
        isStudent = student;
    }

    public UserItem getUser() {
        return user;
    }

    public void setUser(UserItem user) {
        this.user = user;
    }
}
