package com.bbq.module.flashsale.config;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务异常
    LOGIN_FAILED(1001, "账号或密码错误"),
    USER_DISABLED(1002, "账号已被禁用"),
    USERNAME_EXISTS(1003, "账号已存在"),
    PRODUCT_OFF_SHELF(2001, "菜品已下架"),
    STOCK_INSUFFICIENT(2002, "库存不足"),
    FLASH_SALE_EXPIRED(3001, "秒杀活动已结束"),
    FLASH_SALE_SOLD_OUT(3002, "秒杀库存已售罄"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
