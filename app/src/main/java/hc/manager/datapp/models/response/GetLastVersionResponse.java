package hc.manager.datapp.models.response;

import hc.manager.datapp.models.VersionAppDatModel;

public class GetLastVersionResponse extends BaseResponse {
    public VersionAppDatModel verisonAppDat;

    public GetLastVersionResponse(String message, int status) {
        super(message, status);
    }

    public VersionAppDatModel getVerisonAppDat() {
        return verisonAppDat;
    }

    public void setVerisonAppDat(VersionAppDatModel verisonAppDat) {
        this.verisonAppDat = verisonAppDat;
    }
}
