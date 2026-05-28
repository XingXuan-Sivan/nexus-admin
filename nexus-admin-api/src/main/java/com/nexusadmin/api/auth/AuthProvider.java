package com.nexusadmin.api.auth;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.identity.CurrentUserInfo;
import com.nexusadmin.api.domain.identity.LoginRequest;
import com.nexusadmin.api.domain.identity.TokenResponse;
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
     * 验证 Bearer Token 的有效性。
     * <p>
     * 默认实现返回不支持，由具体认证提供者覆盖。
     *
     * @param token   Bearer Token
     * @param context 调用上下文
     * @return 认证结果
     */
    default AuthResult validateToken(String token, InvocationContext context) {
        return AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("Token 验证不受支持")
                .build();
    }

    /**
     * 执行登录操作，返回 Token 信息。
     * <p>
     * 默认实现通过 {@link #authenticate} 向后兼容。
     *
     * @param request 登录请求
     * @param context 调用上下文
     * @return Token 响应，登录失败时返回 null
     */
    default TokenResponse login(LoginRequest request, InvocationContext context) {
        AuthResult result = authenticate(new AuthRequest(request.username(), request.password(), null), context);
        if (result.status() == AuthStatus.SUCCESS) {
            return new TokenResponse("token-" + result.userId(), null, 3600, "Bearer");
        }
        return null;
    }

    /**
     * 执行登出操作，销毁指定 Token。
     * <p>
     * 默认实现返回 false，由具体认证提供者覆盖。
     *
     * @param token   访问令牌
     * @param context 调用上下文
     * @return 是否成功销毁
     */
    default boolean logout(String token, InvocationContext context) {
        return false;
    }

    /**
     * 使用刷新令牌获取新的访问令牌。
     * <p>
     * 默认实现返回 null，由具体认证提供者覆盖。
     *
     * @param refreshToken 刷新令牌
     * @param context      调用上下文
     * @return 新的 Token 响应
     */
    default TokenResponse refresh(String refreshToken, InvocationContext context) {
        return null;
    }

    /**
     * 根据 Token 获取当前用户信息。
     * <p>
     * 默认实现返回 null，由具体认证提供者覆盖。
     *
     * @param token   访问令牌
     * @param context 调用上下文
     * @return 当前用户信息
     */
    default CurrentUserInfo getCurrentUser(String token, InvocationContext context) {
        return null;
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
