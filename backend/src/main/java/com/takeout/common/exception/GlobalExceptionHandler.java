package com.takeout.common.exception;

import com.takeout.common.result.Result;
import com.takeout.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把任意异常统一收敛成 Result，前端永远只处理一种响应结构
 *
 * <p>
 * 分四类处理：业务异常原样转、参数校验异常拼提示、认证/权限异常转 401/403、
 * 兜底异常打日志但**不外泄堆栈**（避免泄露表名、SQL、包结构）。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：Service 主动抛出，属可预期分支，只打 warn 不打堆栈 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{} {}] code={} msg={}",
                request.getMethod(), request.getRequestURI(), e.getResultCode().getCode(), e.getMessage());
        return Result.fail(e.getResultCode().getCode(), e.getMessage());
    }

    /** 未认证：token 缺失/非法/过期 */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("认证异常 [{} {}] code={}",
                request.getMethod(), request.getRequestURI(), e.getResultCode().getCode());
        return Result.fail(e.getResultCode());
    }

    /** 无权限：跨端访问或越权操作 */
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("权限异常 [{} {}] msg={}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(e.getResultCode().getCode(), e.getMessage());
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : msg);
    }

    /** 表单/Query 对象绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : msg);
    }

    /** 方法参数上的 @Validated 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /** 请求体缺失或 JSON 格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    /** 必填请求参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "不支持的请求方法：" + e.getMethod());
    }

    /** 上传文件超限：文件上传是顾客/商家都会走的高频入口，单独给清晰提示 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.FILE_TOO_LARGE);
    }

    /** 兜底：非预期异常必须打全堆栈，但只给前端一句通用提示 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.ERROR);
    }

    /**
     * 路径不存在
     *
     * <p>
     * 必须单独处理：Spring 6 起静态资源找不到会抛 NoResourceFoundException，
     * 若被上面的兜底分支接走，调用方会收到 500（系统异常），
     * 把「URL 写错了」误判成「服务端崩了」，排查方向直接跑偏。
     * </p>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("路径不存在 [{} {}]", request.getMethod(), request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND);
    }
}