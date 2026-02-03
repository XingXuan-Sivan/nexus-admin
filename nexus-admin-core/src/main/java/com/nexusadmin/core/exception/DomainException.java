package com.nexusadmin.core.exception;

/**
 * 领域异常，表示领域逻辑层面的业务规则验证失败或业务约束冲突。
 */
public class DomainException extends CoreException {
    /**
     * 构造带消息的领域异常。
     *
     * @param message 异常消息
     */
    public DomainException(String message) {
        super(message);
    }

    /**
     * 构造带消息和原始异常的领域异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
