package com.nexusadmin.app.auth;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthStatus;
import com.nexusadmin.core.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * 引导认证提供者。
 * <p>
 * 提供基于配置文件的管理员认证能力，作为系统初始化时的默认认证方式。
 * <p>
 * <strong>配置项（platform 作用域）：</strong>
 * <ul>
 *   <li>admin.auth.bootstrap.username - 管理员用户名，默认 admin</li>
 *   <li>admin.auth.bootstrap.password - 管理员密码，默认 admin123</li>
 * </ul>
 */
public class BootstrapAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAuthProvider.class);

    private static final String SCOPE_PLATFORM = "platform";
    private static final String CONFIG_USERNAME = "admin.auth.bootstrap.username";
    private static final String CONFIG_PASSWORD = "admin.auth.bootstrap.password";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final ConfigManager configManager;

    /**
     * 构造引导认证提供者。
     *
     * @param configManager 配置管理器，可为 null（使用默认值）
     */
    public BootstrapAuthProvider(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        String username = getUsername();
        String password = getPassword();

        if (username.equals(request.principal()) && password.equals(request.credential())) {
            log.info("引导认证成功，用户: {}", username);
            return new AuthResult(
                    AuthStatus.SUCCESS,
                    username,
                    "认证成功",
                    Map.of("authType", "bootstrap", "role", "admin")
            );
        }

        log.warn("引导认证失败，用户: {}", request.principal());
        return new AuthResult(
                AuthStatus.FAILED,
                null,
                "用户名或密码错误",
                Map.of()
        );
    }

    /**
     * 获取配置的用户名。
     *
     * @return 用户名
     */
    private String getUsername() {
        if (configManager == null) {
            return DEFAULT_USERNAME;
        }
        return configManager.get(SCOPE_PLATFORM, CONFIG_USERNAME)
                .orElse(DEFAULT_USERNAME);
    }

    /**
     * 获取配置的密码。
     *
     * @return 密码
     */
    private String getPassword() {
        if (configManager == null) {
            return DEFAULT_PASSWORD;
        }
        return configManager.get(SCOPE_PLATFORM, CONFIG_PASSWORD)
                .orElse(DEFAULT_PASSWORD);
    }
}
