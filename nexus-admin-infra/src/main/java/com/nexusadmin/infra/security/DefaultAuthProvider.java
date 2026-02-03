package com.nexusadmin.infra.security;

import com.nexusadmin.core.context.CoreContext;
import com.nexusadmin.core.spi.auth.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 默认认证提供者实现。
 * <p>
 * 基础实现仅用于开发和演示环境：根据请求主体直接构造认证结果，不做真实的身份校验。
 * 生产环境下应通过插件提供更严格的认证实现覆盖本默认实现。
 * </p>
 */
@Component
public class DefaultAuthProvider implements AuthProvider {

    @Override
    public AuthResult authenticate(AuthRequest request, CoreContext context) {
        if (request == null || request.principal() == null || request.principal().isBlank()) {
            return new AuthResult(AuthStatus.FAILED, null, "principal is blank", Map.of());
        }
        return new AuthResult(AuthStatus.SUCCESS, request.principal(), "default auth success", Map.of());
    }
}
