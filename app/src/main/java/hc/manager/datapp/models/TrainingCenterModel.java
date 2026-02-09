package hc.manager.datapp.models;

public class TrainingCenterModel {
    public String id;
    public String code;
    public String name;
    public String address;
    public String phoneNumber;
    public String managerName;
    public boolean teacherSendTc;

    public boolean isTeacherSendTc() {
        return teacherSendTc;
    }

    public void setTeacherSendTc(boolean teacherSendTc) {
        this.teacherSendTc = teacherSendTc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }
}
