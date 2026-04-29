package com.nexusadmin.api.result;

import java.util.Objects;

/**
 * 带数据载荷的统一响应，扩展 {@link Result} 增加泛型数据字段。
 * <p>
 * 用于需要返回业务数据的 API 场景，如查询列表、获取详情等。
 * 无数据载荷的场景（如增删改操作）使用 {@link Result} 即可。
 * <p>
 * <strong>构造方式：</strong>
 * <ul>
 *   <li>静态工厂方法：适用于常规场景，如 {@code DataResult.success(data)}</li>
 *   <li>Builder 模式：适用于需要细粒度定制的场景</li>
 * </ul>
 *
 * @param <T> 数据载荷类型
 */
public class DataResult<T> extends Result {

    /** 数据载荷。 */
    private final T data;

    /**
     * 构造带数据的响应结果。
     *
     * @param code    状态码
     * @param message 消息文本
     * @param data    数据载荷
     */
    private DataResult(int code, String message, T data) {
        super(code, message);
        this.data = data;
    }

    /**
     * 获取数据载荷。
     *
     * @return 数据载荷
     */
    public T getData() {
        return data;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 构造带数据的成功响应。
     *
     * @param data 数据载荷
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> DataResult<T> success(T data) {
        return new DataResult<>(StatusCodes.SUCCESS.code(), StatusCodes.SUCCESS.message(), data);
    }

    /**
     * 构造带自定义消息和数据的成功响应。
     *
     * @param message 消息文本
     * @param data    数据载荷
     * @param <T>     数据类型
     * @return 成功结果
     */
    public static <T> DataResult<T> success(String message, T data) {
        return new DataResult<>(StatusCodes.SUCCESS.code(), message, data);
    }

    /**
     * 根据状态码和数据构造响应。
     *
     * @param statusCode 状态码
     * @param data       数据载荷
     * @param <T>        数据类型
     * @return 响应结果
     */
    public static <T> DataResult<T> of(StatusCode statusCode, T data) {
        Objects.requireNonNull(statusCode, "状态码不能为空");
        return new DataResult<>(statusCode.code(), statusCode.message(), data);
    }

    /**
     * 根据状态码、自定义消息和数据构造响应。
     *
     * @param statusCode 状态码（仅取 code）
     * @param message    自定义消息
     * @param data       数据载荷
     * @param <T>        数据类型
     * @return 响应结果
     */
    public static <T> DataResult<T> of(StatusCode statusCode, String message, T data) {
        Objects.requireNonNull(statusCode, "状态码不能为空");
        return new DataResult<>(statusCode.code(), message, data);
    }

    // ==================== Builder ====================

    /**
     * 创建 Builder 实例。
     * <p>
     * 由于泛型擦除限制，DataResult 不重新声明 {@code builder()} 方法，
     * 而是通过此静态入口创建带泛型的构造器。
     *
     * @param <T> 数据类型
     * @return Builder
     */
    public static <T> Builder<T> dataBuilder() {
        return new Builder<>();
    }

    /**
     * DataResult 构造器。
     *
     * @param <T> 数据类型
     */
    public static class Builder<T> {

        private int code;
        private String message;
        private T data;

        protected Builder() {
        }

        /**
         * 设置状态码数值。
         *
         * @param code 状态码
         * @return 当前 Builder
         */
        public Builder<T> code(int code) {
            this.code = code;
            return this;
        }

        /**
         * 设置消息文本。
         *
         * @param message 消息文本
         * @return 当前 Builder
         */
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        /**
         * 从状态码契约填充 code 和 message。
         *
         * @param statusCode 状态码
         * @return 当前 Builder
         */
        public Builder<T> statusCode(StatusCode statusCode) {
            Objects.requireNonNull(statusCode, "状态码不能为空");
            this.code = statusCode.code();
            this.message = statusCode.message();
            return this;
        }

        /**
         * 设置数据载荷。
         *
         * @param data 数据载荷
         * @return 当前 Builder
         */
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        /**
         * 构造 DataResult 实例。
         *
         * @return 响应结果
         */
        public DataResult<T> build() {
            return new DataResult<>(code, message, data);
        }
    }
}
