package com.schoolmate.exception;

import com.schoolmate.common.Result;
import com.schoolmate.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把各类异常统一转为 Result 响应，避免异常栈直接暴露给前端。
 *
 * @author Albot
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @Valid 校验 Body 参数失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /** 表单绑定 / 校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /** 校验注解作用于单个参数时失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "缺少必填参数：" + e.getParameterName());
    }

    /** 请求体不可读（JSON 格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /** 请求方法不被支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "不支持的请求方法：" + e.getMethod());
    }

    /** 上传文件超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "上传文件超出大小限制（单文件最大 10MB）");
    }

    /**
     * 请求路径不存在。
     *
     * <p>Spring Framework 6.1 起，未命中任何 Controller 的请求会落到兜底的静态资源处理器
     * （{@code ResourceHttpRequestHandler}）上，由它抛出 {@link NoResourceFoundException}。
     * 如果只靠下面的 {@link Exception} 兜底，这条本该是 404 的请求会变成 500
     * 并在日志里打出一整段堆栈 —— 排查时极易被误认成服务端故障，实际只是路径写错。
     * 这里单独拦截，返回 404 并只记一行 WARN。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.warn("接口不存在：{} {}", request.getMethod(), uri);
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：" + request.getMethod() + " " + uri);
    }

    /**
     * 请求路径不存在（映射器层面）。
     *
     * <p>当 {@code spring.mvc.throw-exception-if-no-handler-found=true} 时，
     * DispatcherServlet 会抛 {@link NoHandlerFoundException} 而不是走静态资源兜底，
     * 同样应归为 404。这里一并兜住，避免两种配置下行为不一致。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        log.warn("接口不存在：{} {}", request.getMethod(), uri);
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：" + request.getMethod() + " " + uri);
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("服务器内部错误，uri={}", request.getRequestURI(), e);
        return Result.fail(ResultCode.ERROR.getCode(), "服务器内部错误，请稍后重试");
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + "：" + fieldError.getDefaultMessage();
    }
}
