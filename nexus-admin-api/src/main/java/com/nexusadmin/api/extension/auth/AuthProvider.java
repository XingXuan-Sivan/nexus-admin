package com.nexusadmin.api.extension.auth;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.core.extension.ExtensionPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证扩展点，用于根据凭证对主体进行身份认证，并返回认证结果。
 */
public interface AuthProvider extends ExtensionPoint {

    /**
     * 执行认证逻辑，根据请求和调用上下文返回认证结果。
     *
     * @param request 认证请求
     * @param context 调用上下文
     * @return 认证结果
     */
    AuthResult authenticate(AuthRequest request, InvocationContext context);

    /**
     * 构造认证请求参数。
     *
     * @param principal   主体标识
     * @param credential  凭证
     * @param attributes  认证请求属性
     */
    record AuthRequest(String principal,
                       String credential,
                       Map<String, String> attributes) {
        public AuthRequest {
            attributes = attributes == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(attributes));
        }
    }

    /**
     * 认证结果，封装认证状态、主体标识、消息和扩展属性。
     * <p>
     * 保持内聚性，不合并到通用 Result 体系。
     * 支持通过 {@link Builder} 灵活构建，也支持通过构造函数直接创建。
     *
     * @param status    认证状态
     * @param userId    主体 ID
     * @param message   认证结果消息
     * @param attributes  认证结果属性
     */
    record AuthResult(AuthStatus status,
                      String userId,
                      String message,
                      Map<String, String> attributes) {

        /**
         * 紧凑构造器，保证 attributes 不为 null。
         */
        public AuthResult {
            attributes = attributes == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(attributes));
        }

        /**
         * 创建 Builder 实例。
         *
         * @return Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * AuthResult 构造器，支持链式调用灵活构建认证结果。
         */
        public static class Builder {

            private AuthStatus status;
            private String userId;
            private String message;
            private final Map<String, String> attributes = new HashMap<>();

            /**
             * 设置认证状态。
             *
             * @param status 认证状态
             * @return 当前 Builder
             */
            public Builder status(AuthStatus status) {
                this.status = status;
                return this;
            }

            /**
             * 设置主体 ID。
             *
             * @param userId 主体 ID
             * @return 当前 Builder
             */
            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            /**
             * 设置消息。
             *
             * @param message 消息文本
             * @return 当前 Builder
             */
            public Builder message(String message) {
                this.message = message;
                return this;
            }

            /**
             * 添加单个属性。
             *
             * @param key   属性键
             * @param value 属性值
             * @return 当前 Builder
             */
            public Builder attribute(String key, String value) {
                this.attributes.put(key, value);
                return this;
            }

            /**
             * 批量添加属性。
             *
             * @param attributes 属性映射
             * @return 当前 Builder
             */
            public Builder attributes(Map<String, String> attributes) {
                if (attributes != null) {
                    this.attributes.putAll(attributes);
                }
                return this;
            }

            /**
             * 构造 AuthResult 实例。
             *
             * @return 认证结果
             */
            public AuthResult build() {
                return new AuthResult(status, userId, message, attributes);
            }
        }
    }

    /**
     * 认证状态枚举。
     */
    enum AuthStatus {
        SUCCESS,
        FAILED,
        LOCKED,
        EXPIRED
    }
}
