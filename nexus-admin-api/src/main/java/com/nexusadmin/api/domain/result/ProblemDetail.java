package com.nexusadmin.api.domain.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * RFC 7807 风格错误响应。
 *
 * <p>{@code errorCode} 使用稳定字符串标识；builder 保留 int 重载，仅用于尚未迁移到符号错误码的
 * 非配置模块。字段错误统一使用 JSON Pointer 与结构化校验参数，不返回 rejected value。</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProblemDetail {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final String errorCode;
    private final String traceId;
    private final String scopeId;
    private final List<FieldError> fieldErrors;
    private final String currentRevision;

    private ProblemDetail(Builder builder) {
        this.type = builder.type;
        this.title = builder.title;
        this.status = builder.status;
        this.detail = builder.detail;
        this.instance = builder.instance;
        this.errorCode = builder.errorCode;
        this.traceId = resolveTraceId(builder.traceId);
        this.scopeId = builder.scopeId;
        this.fieldErrors = Collections.unmodifiableList(new ArrayList<>(builder.fieldErrors));
        this.currentRevision = builder.currentRevision;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public String getInstance() {
        return instance;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getScopeId() {
        return scopeId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public String getCurrentRevision() {
        return currentRevision;
    }

    public static ProblemDetail of(String title, int status) {
        return builder().title(title).status(status).errorCode(status).build();
    }

    public static ProblemDetail of(String title, int status, String detail) {
        return builder().title(title).status(status).detail(detail).errorCode(status).build();
    }

    public static ProblemDetail of(String title, int status, int errorCode) {
        return builder().title(title).status(status).errorCode(errorCode).build();
    }

    public static ProblemDetail of(String title, int status, String detail, String errorCode) {
        return builder().title(title).status(status).detail(detail).errorCode(errorCode).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String resolveTraceId(String explicitTraceId) {
        if (explicitTraceId != null && !explicitTraceId.isBlank()) {
            return explicitTraceId;
        }
        String mdcTraceId = MDC.get("traceId");
        return mdcTraceId == null || mdcTraceId.isBlank()
                ? UUID.randomUUID().toString()
                : mdcTraceId;
    }

    /** ProblemDetail 构建器。 */
    public static final class Builder {

        private String type;
        private String title;
        private int status;
        private String detail;
        private String instance;
        private String errorCode;
        private String traceId;
        private String scopeId;
        private List<FieldError> fieldErrors = new ArrayList<>();
        private String currentRevision;

        private Builder() {
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * 兼容尚未迁移的通用状态码调用点；序列化结果仍为字符串。
         */
        public Builder errorCode(int errorCode) {
            return errorCode(Integer.toString(errorCode));
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder scopeId(String scopeId) {
            this.scopeId = scopeId;
            return this;
        }

        public Builder addFieldError(FieldError fieldError) {
            fieldErrors.add(Objects.requireNonNull(fieldError, "字段错误不能为空"));
            return this;
        }

        public Builder fieldErrors(List<FieldError> fieldErrors) {
            this.fieldErrors = fieldErrors == null ? new ArrayList<>() : new ArrayList<>(fieldErrors);
            return this;
        }

        public Builder currentRevision(String currentRevision) {
            this.currentRevision = currentRevision;
            return this;
        }

        public ProblemDetail build() {
            return new ProblemDetail(this);
        }
    }

    /** 配置与请求字段级校验错误。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(
            String path,
            String keyword,
            String messageKey,
            Map<String, Object> params,
            Integer line,
            Integer column
    ) {
        public FieldError {
            params = params == null ? Map.of() : Map.copyOf(params);
        }
    }
}
