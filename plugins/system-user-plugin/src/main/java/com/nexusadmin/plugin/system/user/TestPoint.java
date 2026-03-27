package com.nexusadmin.plugin.system.user;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.core.extension.Extension;
import com.nexusadmin.api.extension.auth.AuthProvider;

@Extension(points = {AuthProvider.class})
public class TestPoint implements AuthProvider {
    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        return null;
    }
}
