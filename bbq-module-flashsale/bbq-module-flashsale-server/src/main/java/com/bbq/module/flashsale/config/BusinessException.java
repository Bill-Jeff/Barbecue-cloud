package com.bbq.module.flashsale.config;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String detail) {
        super(detail);
        this.resultCode = resultCode;
    }
}
