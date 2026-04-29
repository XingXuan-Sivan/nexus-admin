package com.nexusadmin.api.result;

import java.util.Objects;

/**
 * 统一响应基类，封装操作状态码和消息。
 * <p>
 * 作为系统所有 API 响应的标准信封，提供一致的结构化输出。
 * 无数据载荷的场景直接使用 {@link Result}，携带数据的场景使用 {@link DataResult}。
 * <p>
 * <strong>构造方式：</strong>
 * <ul>
 *   <li>静态工厂方法：适用于常规场景，如 {@code Result.success()}、{@code Result.failure(msg)}</li>
 *   <li>Builder 模式：适用于需要细粒度定制的场景</li>
 * </ul>
 */
public class Result {

    /** 状态码。 */
    private final int code;

    /** 消息文本。 */
    private final String message;

    /**
     * 构造响应结果。
     *
     * @param code    状态码
     * @param message 消息文本
     */
    protected Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取消息文本。
     *
     * @return 消息文本
     */
    public String getMessage() {
        return message;
    }

    /**
     * 判断操作是否成功。
     * <p>
     * 状态码在 200-299 范围内视为成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 构造成功响应。
     *
     * @return 成功结果
     */
    public static Result success() {
        return new Result(StatusCodes.SUCCESS.code(), StatusCodes.SUCCESS.message());
    }

    /**
     * 构造带自定义消息的成功响应。
     *
     * @param message 消息文本
     * @return 成功结果
     */
    public static Result success(String message) {
        return new Result(StatusCodes.SUCCESS.code(), message);
    }

    /**
     * 构造失败响应。
     *
     * @return 失败结果
     */
    public static Result failure() {
        return new Result(StatusCodes.INTERNAL_ERROR.code(), StatusCodes.INTERNAL_ERROR.message());
    }

    /**
     * 构造带自定义消息的失败响应。
     *
     * @param message 消息文本
     * @return 失败结果
     */
    public static Result failure(String message) {
        return new Result(StatusCodes.INTERNAL_ERROR.code(), message);
    }

    /**
     * 根据状态码构造响应。
     *
     * @param statusCode 状态码
     * @return 响应结果
     */
    public static Result of(StatusCode statusCode) {
        Objects.requireNonNull(statusCode, "状态码不能为空");
        return new Result(statusCode.code(), statusCode.message());
    }

    /**
     * 根据状态码和自定义消息构造响应。
     *
     * @param statusCode 状态码（仅取 code）
     * @param message    自定义消息
     * @return 响应结果
     */
    public static Result of(StatusCode statusCode, String message) {
        Objects.requireNonNull(statusCode, "状态码不能为空");
        return new Result(statusCode.code(), message);
    }

    // ==================== Builder ====================

    /**
     * 创建 Builder 实例。
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Result 构造器。
     */
    public static class Builder {

        private int code;
        private String message;

        protected Builder() {
        }

        /**
         * 设置状态码数值。
         *
         * @param code 状态码
         * @return 当前 Builder
         */
        public Builder code(int code) {
            this.code = code;
            return this;
        }

        /**
         * 设置消息文本。
         *
         * @param message 消息文本
         * @return 当前 Builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * 从状态码契约填充 code 和 message。
         *
         * @param statusCode 状态码
         * @return 当前 Builder
         */
        public Builder statusCode(StatusCode statusCode) {
            Objects.requireNonNull(statusCode, "状态码不能为空");
            this.code = statusCode.code();
            this.message = statusCode.message();
            return this;
        }

        /**
         * 构造 Result 实例。
         *
         * @return 响应结果
         */
        public Result build() {
            return new Result(code, message);
        }
    }
}
