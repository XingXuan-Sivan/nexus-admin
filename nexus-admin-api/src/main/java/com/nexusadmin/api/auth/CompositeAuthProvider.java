package com.nexusadmin.api.auth;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 组合认证提供者。
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
 *   <li>自动收集所有 AuthProvider Bean（包括插件提供的）</li>
 *   <li>按优先级顺序尝试认证</li>
 *   <li>支持动态扩展（插件可注册新的 AuthProvider）</li>
 *   <li>不感知任何前端实现细节</li>
 * </ul>
 */
public class CompositeAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeAuthProvider.class);

    private final List<AuthProvider> providers;

    /**
     * 构造组合认证提供者。
     * <p>
     * 当传入的提供者列表中包含非 BootstrapAuthProvider 时，自动排除所有 BootstrapAuthProvider。
     *
     * @param providers 认证提供者列表
     */
    public CompositeAuthProvider(List<AuthProvider> providers) {
        this.providers = filterProviders(providers);
        log.info("组合认证提供者已初始化，包含 {} 个认证器", this.providers.size());
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        // 初始化默认失败结果
        AuthResult result = AuthResult.builder()
                .status(AuthStatus.FAILED)
                .message("认证失败")
                .build();

        if (providers.isEmpty()) {
            log.warn("没有可用的认证提供者");
            result = AuthResult.builder()
                    .status(AuthStatus.FAILED)
                    .message("系统未配置认证服务")
                    .build();
            return result;
        }

        for (AuthProvider provider : providers) {
            try {
                result = provider.authenticate(request, context);
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
        return result;
    }

    /**
     * 获取所有认证提供者。
     *
     * @return 认证提供者列表（不可修改）
     */
    public List<AuthProvider> getProviders() {
        return providers;
    }

    /**
     * 根据提供者类型过滤，当存在非引导认证提供者时排除引导认证提供者。
     *
     * @param providers 原始提供者列表
     * @return 过滤后的不可修改列表
     */
    private List<AuthProvider> filterProviders(List<AuthProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return List.of();
        }

        boolean hasNonBootstrap = providers.stream()
                .anyMatch(p -> !(p instanceof BootstrapAuthProvider));

        if (!hasNonBootstrap) {
            return Collections.unmodifiableList(new ArrayList<>(providers));
        }

        List<AuthProvider> filtered = providers.stream()
                .filter(p -> !(p instanceof BootstrapAuthProvider))
                .toList();

        log.info("检测到其他认证提供者，引导认证已自动禁用");
        return filtered;
    }
}
