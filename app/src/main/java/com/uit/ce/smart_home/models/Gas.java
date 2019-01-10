package com.uit.ce.smart_home.models;

public class Gas extends Device{
    public String status;

    public Gas(String name, String type, String status) {
        super(name, type);
        this.status = status;
    }

    public Gas(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
