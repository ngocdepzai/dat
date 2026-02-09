package hc.manager.datapp.models.response;

public class CheckUserLoginResponse extends BaseResponse {
    public boolean canLogin;

    public CheckUserLoginResponse(String message, int status) {
        super(message, status);
    }

    public boolean isCanLogin() {
        return canLogin;
    }

    public void setCanLogin(boolean canLogin) {
        this.canLogin = canLogin;
    }
}
