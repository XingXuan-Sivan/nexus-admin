package com.nexusadmin.plugin.system.user;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.auth.AuthProvider;

/**
 * AuthProvider 测试桩。
 * 仅用于拓展点机制的单元测试验证，不作为正式扩展注册。
 */
public class TestPoint implements AuthProvider {
    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        return null;
    }
}
