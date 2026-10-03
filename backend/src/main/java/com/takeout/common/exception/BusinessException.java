package com.takeout.common.exception;

import com.takeout.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：可预期的业务规则不满足，由 GlobalExceptionHandler 统一转 Result
 *
 * <p>约定：Service 层校验失败一律抛它，不要返回 null 让 Controller 猜。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}