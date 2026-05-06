package com.nexusadmin.api.auth;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthStatus;
import com.nexusadmin.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 引导认证提供者。
 * <p>
 * 提供基于配置中心的管理员认证能力，作为系统初始化时的默认认证方式。
 * 当存在其他非引导认证提供者时，引导认证将被 CompositeAuthProvider 自动排除，
 * 类似于 Spring Security 的默认登录页在有其他实现时被覆盖的机制。
 * <p>
 * <strong>配置项（platform 作用域）：</strong>
 * <ul>
 *   <li>bootstrapUsername - 管理员用户名，默认 admin</li>
 *   <li>bootstrapPassword - 管理员密码，默认 admin123</li>
 * </ul>
 * <p>
 * 凭据每次从 ConfigManager 动态读取，支持运行时热更新。
 */
public class BootstrapAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAuthProvider.class);

    private static final String SCOPE = "platform";
    private static final String KEY_USERNAME = "bootstrapUsername";
    private static final String KEY_PASSWORD = "bootstrapPassword";

    private final ConfigManager configManager;

    /**
     * 构造引导认证提供者。
     *
     * @param configManager 配置管理器，用于动态读取管理员凭据
     */
    public BootstrapAuthProvider(ConfigManager configManager) {
        this.configManager = configManager;
        log.info("引导认证提供者已初始化");
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        String username = configManager.get(SCOPE, KEY_USERNAME).orElse("admin");
        String password = configManager.get(SCOPE, KEY_PASSWORD).orElse("admin123");

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

    /**
     * 验证引导 Token。
     * <p>
     * 与 {@link #login} 默认实现生成的 "token-{userId}" 格式 Token 配套，
     * 提取 Token 中的用户标识并与配置中心的用户名比对。
     *
     * @param token   Bearer Token
     * @param context 调用上下文
     * @return 认证结果
     */
    @Override
    public AuthResult validateToken(String token, InvocationContext context) {
        if (token == null || !token.startsWith("token-")) {
            return AuthResult.builder()
                    .status(AuthStatus.FAILED)
                    .message("Token 格式无效")
                    .build();
        }

        String userId = token.substring("token-".length());
        String username = configManager.get(SCOPE, KEY_USERNAME).orElse("admin");

        if (username.equals(userId)) {
            return AuthResult.builder()
                    .status(AuthStatus.SUCCESS)
                    .userId(userId)
                    .message("Token 验证通过")
                    .attribute("authType", "bootstrap")
                    .attribute("role", "admin")
                    .build();
        }

        log.warn("引导 Token 验证失败，Token 用户: {}", userId);
        return AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("Token 无效")
                .build();
    }
}
