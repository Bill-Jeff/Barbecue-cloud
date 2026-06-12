package com.bbq.module.flashsale.config;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null);
    }

    public static <T> Result<T> error(ResultCode rc, String detail) {
        return new Result<>(rc.getCode(), detail, null);
    }
}
