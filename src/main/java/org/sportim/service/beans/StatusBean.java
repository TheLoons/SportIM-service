package org.sportim.service.beans;

/**
 * Created by hannah on 12/4/14.
 */
public class StatusBean {
    private int code;
    private String message;

    public StatusBean() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
