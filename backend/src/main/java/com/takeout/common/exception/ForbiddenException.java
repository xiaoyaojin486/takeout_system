package com.takeout.common.exception;

import com.takeout.common.result.ResultCode;
import lombok.Getter;

/**
 * 无权限异常：已登录但角色/数据归属不匹配（跨端访问、越权操作他人数据）
 */
@Getter
public class ForbiddenException extends RuntimeException {

    private final ResultCode resultCode;

    public ForbiddenException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public ForbiddenException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}