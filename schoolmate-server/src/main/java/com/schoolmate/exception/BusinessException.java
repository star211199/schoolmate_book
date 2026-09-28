package com.schoolmate.exception;

import com.schoolmate.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由 Service 层主动抛出，由全局异常处理器转换为统一响应。
 *
 * @author Albot
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
