package hc.manager.datapp.models.response;

public class UploadAuthResponse extends BaseResponse {
    public String filePath;

    public UploadAuthResponse(String message, int status) {
        super(message, status);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
