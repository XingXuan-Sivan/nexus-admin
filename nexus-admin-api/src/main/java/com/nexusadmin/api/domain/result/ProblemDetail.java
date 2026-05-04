package com.nexusadmin.api.domain.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RFC 7807 Problem Details 标准实现，用于描述 API 错误响应的详细信息。
 * <p>
 * 提供问题类型 URI、标题、HTTP 状态码、详细描述、发生实例等标准字段，
 * 并扩展业务错误码和字段级校验错误列表，便于前端和客户端精准定位问题。
 * <p>
 * <strong>构造方式：</strong>
 * <ul>
 *   <li>静态工厂方法：适用于快速构建，如 {@code ProblemDetail.of(title, status)}</li>
 *   <li>Builder 模式：适用于需要完整定制的场景</li>
 * </ul>
 */
public class ProblemDetail {

    /** 问题类型 URI，用于标识问题种类。 */
    private final String type;

    /** 人类可读的简短标题。 */
    private final String title;

    /** HTTP 状态码。 */
    private final int status;

    /** 详细描述。 */
    private final String detail;

    /** 问题发生的 URI 实例。 */
    private final String instance;

    /** 业务错误码，对应 {@link StatusCodes} 的编号。 */
    private final int errorCode;

    /** 字段级校验错误列表。 */
    private final List<FieldError> fieldErrors;

    private ProblemDetail(String type, String title, int status, String detail, String instance,
                          int errorCode, List<FieldError> fieldErrors) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    /**
     * 获取问题类型 URI。
     *
     * @return 问题类型 URI
     */
    public String getType() {
        return type;
    }

    /**
     * 获取标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 获取详细描述。
     *
     * @return 详细描述
     */
    public String getDetail() {
        return detail;
    }

    /**
     * 获取问题发生的 URI 实例。
     *
     * @return URI 实例
     */
    public String getInstance() {
        return instance;
    }

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 获取字段级校验错误列表。
     *
     * @return 字段错误列表
     */
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 构造基础 ProblemDetail。
     *
     * @param title  标题
     * @param status HTTP 状态码
     * @return ProblemDetail 实例
     */
    public static ProblemDetail of(String title, int status) {
        return new ProblemDetail(null, title, status, null, null, 0, Collections.emptyList());
    }

    /**
     * 构造带详细描述的 ProblemDetail。
     *
     * @param title  标题
     * @param status HTTP 状态码
     * @param detail 详细描述
     * @return ProblemDetail 实例
     */
    public static ProblemDetail of(String title, int status, String detail) {
        return new ProblemDetail(null, title, status, detail, null, 0, Collections.emptyList());
    }

    /**
     * 构造带业务错误码的 ProblemDetail。
     *
     * @param title     标题
     * @param status    HTTP 状态码
     * @param errorCode 业务错误码
     * @return ProblemDetail 实例
     */
    public static ProblemDetail of(String title, int status, int errorCode) {
        return new ProblemDetail(null, title, status, null, null, errorCode, Collections.emptyList());
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
     * ProblemDetail 构造器。
     */
    public static class Builder {

        private String type;
        private String title;
        private int status;
        private String detail;
        private String instance;
        private int errorCode;
        private List<FieldError> fieldErrors = new ArrayList<>();

        protected Builder() {
        }

        /**
         * 设置问题类型 URI。
         *
         * @param type 问题类型 URI
         * @return 当前 Builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * 设置标题。
         *
         * @param title 标题
         * @return 当前 Builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * 设置 HTTP 状态码。
         *
         * @param status HTTP 状态码
         * @return 当前 Builder
         */
        public Builder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * 设置详细描述。
         *
         * @param detail 详细描述
         * @return 当前 Builder
         */
        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        /**
         * 设置问题发生的 URI 实例。
         *
         * @param instance URI 实例
         * @return 当前 Builder
         */
        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        /**
         * 设置业务错误码。
         *
         * @param errorCode 业务错误码
         * @return 当前 Builder
         */
        public Builder errorCode(int errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * 添加字段级校验错误。
         *
         * @param fieldError 字段错误
         * @return 当前 Builder
         */
        public Builder addFieldError(FieldError fieldError) {
            Objects.requireNonNull(fieldError, "字段错误不能为空");
            this.fieldErrors.add(fieldError);
            return this;
        }

        /**
         * 设置字段级校验错误列表。
         *
         * @param fieldErrors 字段错误列表
         * @return 当前 Builder
         */
        public Builder fieldErrors(List<FieldError> fieldErrors) {
            this.fieldErrors = fieldErrors != null ? new ArrayList<>(fieldErrors) : new ArrayList<>();
            return this;
        }

        /**
         * 构造 ProblemDetail 实例。
         *
         * @return ProblemDetail 实例
         */
        public ProblemDetail build() {
            return new ProblemDetail(type, title, status, detail, instance, errorCode,
                    Collections.unmodifiableList(new ArrayList<>(fieldErrors)));
        }
    }

    /**
     * 字段级校验错误。
     */
    public static class FieldError {

        /** 字段路径。 */
        private final String field;

        /** 实际值。 */
        private final Object value;

        /** 错误信息。 */
        private final String message;

        /**
         * 构造字段错误。
         *
         * @param field   字段路径
         * @param value   实际值
         * @param message 错误信息
         */
        public FieldError(String field, Object value, String message) {
            this.field = field;
            this.value = value;
            this.message = message;
        }

        /**
         * 获取字段路径。
         *
         * @return 字段路径
         */
        public String getField() {
            return field;
        }

        /**
         * 获取实际值。
         *
         * @return 实际值
         */
        public Object getValue() {
            return value;
        }

        /**
         * 获取错误信息。
         *
         * @return 错误信息
         */
        public String getMessage() {
            return message;
        }
    }
}
