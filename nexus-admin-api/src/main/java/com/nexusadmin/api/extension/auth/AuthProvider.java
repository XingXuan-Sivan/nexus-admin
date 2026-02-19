package com.nexusadmin.api.extension.auth;

import com.nexusadmin.api.context.CoreContext;
import com.nexusadmin.core.extension.ExtensionPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证扩展点，用于根据凭证对主体进行身份认证，并返回认证结果。
 */
public interface AuthProvider extends ExtensionPoint {

    /**
     * 执行认证逻辑，根据请求和平台上下文返回认证结果。
     *
     * @param request 认证请求
     * @param context 平台上下文
     * @return 认证结果
     */
    AuthResult authenticate(AuthRequest request, CoreContext context);

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
     * 构造认证结果。
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
        public AuthResult {
            attributes = attributes == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(attributes));
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
