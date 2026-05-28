package com.nexusadmin.api.auth;

import com.nexusadmin.api.auth.impl.BootstrapAuthProvider;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.identity.CurrentUserInfo;
import com.nexusadmin.api.domain.identity.LoginRequest;
import com.nexusadmin.api.domain.identity.TokenResponse;
import com.nexusadmin.api.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.auth.AuthProvider.AuthStatus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 组合认证提供者（认证调度器）。
 * <p>
 * 按顺序委托给所有注册的 AuthProvider 进行认证，返回第一个成功的结果。
 * 如果所有提供者都失败，则返回失败结果。
 * <p>
 * <strong>自动排除机制：</strong>
 * 当存在非 {@link BootstrapAuthProvider} 的认证提供者时，自动排除 BootstrapAuthProvider，
 * 类似于 Spring Security 的默认登录页在有自定义实现时被覆盖的机制。
 * <p>
 * <strong>设计要点：</strong>
 * <ul>
 *   <li>通过 {@link ExtensionConsumer} 动态获取 AuthProvider 列表，支持运行时热替换</li>
 *   <li>按优先级顺序尝试认证</li>
 *   <li>不实现 AuthProvider 接口（它是调度器/消费者，不是认证提供者）</li>
 *   <li>不感知任何前端实现细节</li>
 * </ul>
 */
public class CompositeAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeAuthProvider.class);

    private final ExtensionConsumer<AuthProvider> authProviderConsumer;

    /**
     * 构造组合认证提供者。
     *
     * @param authProviderConsumer 认证提供者扩展点消费者
     */
    public CompositeAuthProvider(ExtensionConsumer<AuthProvider> authProviderConsumer) {
        this.authProviderConsumer = authProviderConsumer;
    }

    /**
     * 执行认证，按优先级依次委托给所有活跃的认证提供者。
     *
     * @param request 认证请求
     * @param context 调用上下文
     * @return 认证结果
     */
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        List<AuthProvider> providers = resolveActiveProviders();

        if (providers.isEmpty()) {
            log.warn("没有可用的认证提供者");
            return AuthResult.builder()
                    .status(AuthStatus.FAILED)
                    .message("没有可用的认证提供者")
                    .build();
        }

        for (AuthProvider provider : providers) {
            try {
                AuthResult result = provider.authenticate(request, context);
                if (result.status() == AuthStatus.SUCCESS) {
                    if (log.isDebugEnabled()) {
                        log.debug("认证成功，提供者: {}", provider.getClass().getSimpleName());
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("认证提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.debug("所有认证提供者均认证失败，用户: {}", request.principal());
        return AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("认证失败")
                .build();
    }

    /**
     * 验证 Bearer Token，按优先级依次委托给所有活跃的认证提供者。
     *
     * @param token   Bearer Token
     * @param context 调用上下文
     * @return 认证结果
     */
    public AuthResult validateToken(String token, InvocationContext context) {
        List<AuthProvider> providers = resolveActiveProviders();

        if (providers.isEmpty()) {
            log.warn("没有可用的认证提供者");
            return AuthResult.builder()
                    .status(AuthStatus.FAILED)
                    .message("没有可用的认证提供者")
                    .build();
        }

        for (AuthProvider provider : providers) {
            try {
                AuthResult result = provider.validateToken(token, context);
                if (result.status() == AuthStatus.SUCCESS) {
                    if (log.isDebugEnabled()) {
                        log.debug("Token 验证成功，提供者: {}", provider.getClass().getSimpleName());
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("Token 验证提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.debug("所有认证提供者均 Token 验证失败");
        return AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("Token 无效或已过期")
                .build();
    }

    /**
     * 执行登录，按优先级依次委托给所有活跃的认证提供者。
     *
     * @param request 登录请求
     * @param context 调用上下文
     * @return Token 响应，所有提供者均失败时返回 null
     */
    public TokenResponse login(LoginRequest request, InvocationContext context) {
        List<AuthProvider> providers = resolveActiveProviders();

        for (AuthProvider provider : providers) {
            try {
                TokenResponse response = provider.login(request, context);
                if (response != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("登录成功，提供者: {}", provider.getClass().getSimpleName());
                    }
                    return response;
                }
            } catch (Exception e) {
                log.warn("登录提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.debug("所有认证提供者均登录失败，用户: {}", request.username());
        return null;
    }

    /**
     * 执行登出，通知所有活跃的认证提供者销毁 Token。
     *
     * @param token   访问令牌
     * @param context 调用上下文
     * @return 是否有提供者成功处理登出
     */
    public boolean logout(String token, InvocationContext context) {
        List<AuthProvider> providers = resolveActiveProviders();

        for (AuthProvider provider : providers) {
            try {
                if (provider.logout(token, context)) {
                    if (log.isDebugEnabled()) {
                        log.debug("登出成功，提供者: {}", provider.getClass().getSimpleName());
                    }
                    return true;
                }
            } catch (Exception e) {
                log.warn("登出提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.debug("所有认证提供者均未处理登出");
        return false;
    }

    /**
     * 刷新访问令牌，按优先级依次委托给所有活跃的认证提供者。
     *
     * @param refreshToken 刷新令牌
     * @param context      调用上下文
     * @return 新的 Token 响应，所有提供者均失败时返回 null
     */
    public TokenResponse refresh(String refreshToken, InvocationContext context) {
        List<AuthProvider> providers = resolveActiveProviders();

        for (AuthProvider provider : providers) {
            try {
                TokenResponse response = provider.refresh(refreshToken, context);
                if (response != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("刷新 Token 成功，提供者: {}", provider.getClass().getSimpleName());
                    }
                    return response;
                }
            } catch (Exception e) {
                log.warn("刷新 Token 提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.debug("所有认证提供者均刷新 Token 失败");
        return null;
    }

    /**
     * 获取当前用户信息，按优先级依次委托给所有活跃的认证提供者。
     *
     * @param token   访问令牌
     * @param context 调用上下文
     * @return 当前用户信息，所有提供者均失败时返回 null
     */
    public CurrentUserInfo getCurrentUser(String token, InvocationContext context) {
        List<AuthProvider> providers = resolveActiveProviders();

        for (AuthProvider provider : providers) {
            try {
                CurrentUserInfo user = provider.getCurrentUser(token, context);
                if (user != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("获取用户信息成功，提供者: {}", provider.getClass().getSimpleName());
                    }
                    return user;
                }
            } catch (Exception e) {
                log.warn("获取用户信息提供者执行异常: {} - {}",
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.debug("所有认证提供者均未获取到用户信息");
        return null;
    }

    /**
     * 获取活跃的认证提供者列表（自动降级逻辑）。
     * <p>
     * 当存在非引导认证提供者时，自动排除引导认证提供者，实现引导认证的自动降级；
     * 当非引导认证被卸载后，引导认证自动恢复。
     *
     * @return 活跃的认证提供者列表
     */
    private List<AuthProvider> resolveActiveProviders() {
        List<AuthProvider> all = authProviderConsumer.getAll();

        boolean hasNonBootstrap = all.stream()
                .anyMatch(p -> !(p instanceof BootstrapAuthProvider));

        if (hasNonBootstrap) {
            List<AuthProvider> filtered = all.stream()
                    .filter(p -> !(p instanceof BootstrapAuthProvider))
                    .toList();
            log.debug("检测到非引导认证提供者，引导认证已自动降级");
            return filtered;
        }

        return all;
    }
}
