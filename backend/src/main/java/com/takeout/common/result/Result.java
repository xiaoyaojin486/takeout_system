package com.takeout.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体：Controller 一律返回 Result<VO> 或 Result<PageResult<VO>>
 */
@Data
public class Result<T> implements Serializable {

    /** 业务码，200 表示成功，其余见 ResultCode */
    private Integer code;

    /** 提示信息，前端直接弹窗展示 */
    private String msg;

    /** 业务数据 */
    private T data;

    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return build(ResultCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String msg) {
        return build(resultCode.getCode(), msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return build(code, msg, null);
    }

    private static <T> Result<T> build(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}