package com.takeout.common.exception;

import com.takeout.common.result.ResultCode;
import lombok.Getter;

/**
 * 未认证异常：token 缺失/非法/过期，统一由过滤器与全局异常处理器转 401
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final ResultCode resultCode;

    public UnauthorizedException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }
}