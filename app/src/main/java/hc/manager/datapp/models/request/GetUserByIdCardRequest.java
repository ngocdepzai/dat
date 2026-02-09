package hc.manager.datapp.models.request;

public class GetUserByIdCardRequest {
    public String idCard;
    public String seri;
    public String codeOrIdNo;
    public String seriCard;

    public GetUserByIdCardRequest(String idCard, String seri, String code) {
        this.idCard = idCard;
        this.seri = seri;
        this.codeOrIdNo = code;
    }

    public GetUserByIdCardRequest(String idCard, String seri) {
        this.idCard = idCard;
        this.seri = seri;
        this.codeOrIdNo = null;
    }

    public GetUserByIdCardRequest(String _seri) {
        this.idCard = null;
        this.seri = _seri;
    }

    public String getSeriCard() {
        return seriCard;
    }

    public void setSeriCard(String seriCard) {
        this.seriCard = seriCard;
    }

    public String getCodeOrIdNo() {
        return codeOrIdNo;
    }

    public void setCodeOrIdNo(String codeOrIdNo) {
        this.codeOrIdNo = codeOrIdNo;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getSeri() {
        return seri;
    }

    public void setSeri(String seri) {
        this.seri = seri;
    }
}
