package hc.manager.datapp.models.response;

import hc.manager.datapp.models.ResetDeviceModel;

public class GetResetDeviceResponse extends BaseResponse {
    public ResetDeviceModel resetDevice;

    public GetResetDeviceResponse(String message, int status) {
        super(message, status);
    }

    public ResetDeviceModel getResetDevice() {
        return resetDevice;
    }

    public void setResetDevice(ResetDeviceModel resetDevice) {
        this.resetDevice = resetDevice;
    }
}
