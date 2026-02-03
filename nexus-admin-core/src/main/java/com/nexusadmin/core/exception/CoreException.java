package com.nexusadmin.core.exception;

/**
 * 核心模块基础异常，作为所有核心领域异常的父类。
 */
public class CoreException extends RuntimeException {
    /**
     * 构造带消息的异常。
     *
     * @param message 异常消息
     */
    public CoreException(String message) {
        super(message);
    }

    /**
     * 构造带消息和原始异常的异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public CoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
