package com.nexusadmin.app.auth;

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
 * <strong>设计要点：</strong>
 * <ul>
 *   <li>自动收集所有 AuthProvider Bean（包括插件提供的）</li>
 *   <li>按优先级顺序尝试认证</li>
 *   <li>支持动态扩展（插件可注册新的 AuthProvider）</li>
 * </ul>
 */
public class CompositeAuthProvider implements AuthProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeAuthProvider.class);

    private final List<AuthProvider> providers;

    /**
     * 构造组合认证提供者。
     *
     * @param providers 认证提供者列表
     */
    public CompositeAuthProvider(List<AuthProvider> providers) {
        this.providers = providers != null 
                ? Collections.unmodifiableList(new ArrayList<>(providers))
                : List.of();
        log.info("组合认证提供者已初始化，包含 {} 个认证器", this.providers.size());
    }

    @Override
    public AuthResult authenticate(AuthRequest request, InvocationContext context) {
        if (providers.isEmpty()) {
            log.warn("没有可用的认证提供者");
            return new AuthResult(
                    AuthStatus.FAILED,
                    null,
                    "系统未配置认证服务",
                    null
            );
        }

        // 按顺序尝试每个认证提供者
        for (AuthProvider provider : providers) {
            try {
                AuthResult result = provider.authenticate(request, context);
                if (result.status() == AuthStatus.SUCCESS) {
                    log.debug("认证成功，提供者: {}", provider.getClass().getSimpleName());
                    return result;
                }
            } catch (Exception e) {
                log.warn("认证提供者执行异常: {} - {}", 
                        provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.warn("所有认证提供者均认证失败，用户: {}", request.principal());
        return new AuthResult(
                AuthStatus.FAILED,
                null,
                "认证失败",
                null
        );
    }

    /**
     * 获取所有认证提供者。
     *
     * @return 认证提供者列表（不可修改）
     */
    public List<AuthProvider> getProviders() {
        return providers;
    }
}
