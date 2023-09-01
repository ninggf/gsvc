package com.apzda.cloud.demo.bar.error;

import com.apzda.cloud.gsvc.IServiceError;

public enum BarError implements IServiceError {

    ;

    public final int code;

    public final String message;

    BarError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

}
