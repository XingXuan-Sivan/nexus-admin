package com.nexusadmin.core.config.schema;

import java.util.Objects;

/**
 * Schema 验证消息，描述单条验证错误信息。
 * <p>作为 {@link SchemaValidator} 接口的返回类型，与具体验证实现解耦。</p>
 */
public final class ValidationMessage {

    /**
     * 验证关键字（如 "type"、"required"）。
     */
    private final String keyword;

    /**
     * 错误路径（如 "$.properties.name"）。
     */
    private final String path;

    /**
     * 错误描述信息。
     */
    private final String message;

    /**
     * 构造验证消息。
     *
     * @param keyword 验证关键字
     * @param path    错误路径
     * @param message 错误描述信息
     */
    public ValidationMessage(String keyword, String path, String message) {
        this.keyword = keyword;
        this.path = path;
        this.message = Objects.requireNonNull(message, "错误描述不能为空");
    }

    /**
     * 获取验证关键字。
     *
     * @return 关键字，可能为 null
     */
    public String keyword() {
        return keyword;
    }

    /**
     * 获取错误路径。
     *
     * @return 错误路径，可能为 null
     */
    public String path() {
        return path;
    }

    /**
     * 获取错误描述信息。
     *
     * @return 错误描述
     */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        if (path != null && !path.isEmpty()) {
            return path + ": " + message;
        }
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValidationMessage that)) return false;
        return Objects.equals(keyword, that.keyword)
                && Objects.equals(path, that.path)
                && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword, path, message);
    }
}
