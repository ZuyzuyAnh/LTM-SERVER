package org.example.dto;

// Message la kieu du lieu de giao tiep giua client va server
public class Message {
    private int sender;
    private String action;
    private String data;
    private String errorMsg;

    public Message(int sender, String action, String data, String errorMsg) {
        this.sender = sender;
        this.action = action;
        this.data = data;
        this.errorMsg = errorMsg;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
