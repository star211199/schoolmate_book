package com.schoolmate.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码。
 *
 * <p>语义分段：
 * <ul>
 *   <li>20000 成功</li>
 *   <li>40001 参数校验失败</li>
 *   <li>40100 未认证 / Token 失效</li>
 *   <li>40300 无权限（越权访问）</li>
 *   <li>40400 资源不存在</li>
 *   <li>40900 资源冲突（如重复加入班级）</li>
 *   <li>50000 服务器内部错误</li>
 * </ul>
 *
 * @author Albot
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(20000, "操作成功"),
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40100, "未认证或登录已过期"),
    FORBIDDEN(40300, "无权限执行该操作"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    ERROR(50000, "服务器内部错误");

    private final Integer code;
    private final String message;
}
