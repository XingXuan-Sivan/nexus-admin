package com.nexusadmin.api.auth;

import com.nexusadmin.api.config.properties.BootstrapAuthProperties;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 引导认证提供者。
 * <p>
 * 提供基于配置文件的管理员认证能力，作为系统初始化时的默认认证方式。
 * 当存在其他非引导认证提供者时，引导认证将被 CompositeAuthProvider 自动排除，
 * 类似于 Spring Security 的默认登录页在有其他实现时被覆盖的机制。
 * <p>
 * <strong>配置项（platform.auth.bootstrap 前缀）：</strong>
 * <ul>
 *   <li>platform.auth.bootstrap.username - 管理员用户名，默认 admin</li>
 *   <li>platform.auth.bootstrap.password - 管理员密码，默认 admin123</li>
 * </ul>
 */
public class BootstrapAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAuthProvider.class);

    private final String username;
    private final String password;

    /**
     * 构造引导认证提供者。
     *
     * @param properties 引导认证配置属性，从中读取管理员凭据
     */
    public BootstrapAuthProvider(BootstrapAuthProperties properties) {
        this.username = properties.getUsername();
        this.password = properties.getPassword();
        log.info("引导认证提供者已初始化，用户: {}", username);
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        if (username.equals(request.principal()) && password.equals(request.credential())) {
            return AuthResult.builder()
                    .status(AuthStatus.SUCCESS)
                    .userId(username)
                    .message("认证成功")
                    .attribute("authType", "bootstrap")
                    .attribute("role", "admin")
                    .build();
        }

        log.warn("引导认证失败，用户: {}", request.principal());
        return AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("用户名或密码错误")
                .build();
    }
}
