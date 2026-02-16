package com.nexusadmin.api.exception;

/**
 * 插件描述文件解析异常。
 * <p>表示 plugin.json 格式错误、缺失字段、值非法等问题。</p>
 */
public class DescriptorParseException extends RuntimeException {

    /**
     * 使用指定消息构造解析异常。
     *
     * @param message 异常消息
     */
    public DescriptorParseException(String message) {
        super(message);
    }

    /**
     * 使用指定消息和原因构造解析异常。
     *
     * @param message 异常消息
     * @param cause   原始异常原因
     */
    public DescriptorParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
