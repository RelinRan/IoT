package androidx.iot.utils;

import androidx.annotation.NonNull;

public class Value {

    private int code;
    private String message;

    public Value() {

    }

    public Value(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public String toString() {
        return "{\"code\":\"" + code + "\"," + "\"message\":\"" + message + "\"}";
    }
}
