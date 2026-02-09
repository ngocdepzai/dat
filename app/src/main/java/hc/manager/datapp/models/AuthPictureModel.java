package hc.manager.datapp.models;

import java.io.File;

public class AuthPictureModel {
    public String time;
    public String status;
    public String filePathLocal;
    public File fileLocal;
    public long Date;
    public int Type;

    public AuthPictureModel(String time, String status) {
        this.time = time;
        this.status = status;
        this.Date = System.currentTimeMillis() / 1000;
    }

    public AuthPictureModel(String time) {
        this.time = time;
        this.Date = System.currentTimeMillis() / 1000;
    }

    public AuthPictureModel() {
    }

    public String getFilePathLocal() {
        return filePathLocal;
    }

    public void setFilePathLocal(String filePathLocal) {
        this.filePathLocal = filePathLocal;
    }

    public File getFileLocal() {
        return fileLocal;
    }

    public void setFileLocal(File fileLocal) {
        this.fileLocal = fileLocal;
    }

    public int getType() {
        return Type;
    }

    public void setType(int type) {
        Type = type;
    }

    public long getDate() {
        return Date;
    }

    public void setDate(long date) {
        Date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
