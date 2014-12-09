package org.sportim.service.beans;

/**
 * Created by hannah on 12/4/14.
 */
public class StatusBean {
    private int code;
    private String message;

    public StatusBean() {
    }

    public StatusBean(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        if (this.message == null) {
            this.message = "";
        }
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
