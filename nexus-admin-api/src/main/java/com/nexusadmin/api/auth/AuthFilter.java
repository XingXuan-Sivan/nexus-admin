package com.nexusadmin.api.auth;

import com.nexusadmin.api.auth.impl.DefaultAuthChallengeHandler;
import com.nexusadmin.api.config.properties.PanelWebProperties;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.auth.AuthProvider.AuthStatus;
import com.nexusadmin.api.util.HttpAuthUtils;
import com.nexusadmin.core.extension.ExtensionConsumer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * 管理面板认证过滤器。
 * <p>
 * 拦截管理面板基路径（默认 /admin/**）下的所有请求，执行认证检查。
 * 基路径不含版本号，确保新增 API 版本时无需修改过滤器配置，符合开闭原则。
 * <p>
 * <strong>凭证来源优先级：</strong>
 * <ol>
 *   <li>Session 中已认证的用户信息</li>
 *   <li>Bearer Token（Authorization: Bearer xxx）</li>
 *   <li>Basic 认证头（Authorization: Basic xxx）</li>
 * </ol>
 * <p>
 * <strong>公开端点（无需认证）：</strong>
 * 由 {@link PanelWebProperties#getPublicEndpoints()} 配置，支持 Ant 风格路径模式，
 * 默认包含登录端点、API 文档端点及静态资源路径。
 * <p>
 * <strong>认证失败处理：</strong>
 * 委托给 {@link AuthChallengeHandler} 扩展点，默认使用 {@link DefaultAuthChallengeHandler}
 * 返回标准 401 响应。
 */
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final PanelWebProperties properties;
    private final CompositeAuthProvider authProvider;
    private final ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer;
    private final AuthChallengeHandler fallbackHandler;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 构造管理面板认证过滤器。
     *
     * @param properties                管理面板 Web 配置属性
     * @param authProvider              组合认证提供者
     * @param challengeHandlerConsumer   认证挑战处理器扩展点消费者
     */
    public AuthFilter(PanelWebProperties properties,
                      CompositeAuthProvider authProvider,
                      ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer) {
        this.properties = properties;
        this.authProvider = authProvider;
        this.challengeHandlerConsumer = challengeHandlerConsumer;
        this.fallbackHandler = new DefaultAuthChallengeHandler();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("管理面板认证过滤器已初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // 只拦截管理面板基路径（如 /admin/**），不感知版本号
        if (!requestPath.startsWith(properties.getBasePath())) {
            chain.doFilter(request, response);
            return;
        }

        // 公开端点放行（由 yml 配置，支持 Ant 风格模式）
        if (isPublicEndpoint(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 检查 Session 中是否已认证
        String sessionUser = HttpAuthUtils.getSessionUser(httpRequest);
        if (sessionUser != null) {
            log.debug("管理面板会话认证通过，用户: {}", sessionUser);
            chain.doFilter(request, response);
            return;
        }

        // 2. 尝试 Bearer Token 认证
        String bearerToken = HttpAuthUtils.extractBearerToken(httpRequest);
        if (bearerToken != null) {
            AuthResult authResult = authProvider.validateToken(bearerToken, buildContext(httpRequest));
            if (authResult.status() == AuthStatus.SUCCESS) {
                log.debug("管理面板 Bearer Token 认证通过，用户: {}", authResult.userId());
                HttpAuthUtils.setSessionUser(httpRequest, authResult.userId());
                chain.doFilter(request, response);
                return;
            }
            log.warn("管理面板 Bearer Token 认证失败: {}", authResult.message());
            resolveChallengeHandler().handleChallenge(httpRequest, httpResponse, authResult.message());
            return;
        }

        // 3. 尝试 Basic 认证
        String authHeader = httpRequest.getHeader("Authorization");
        String[] basicCredentials = HttpAuthUtils.parseBasicAuth(authHeader);
        if (basicCredentials != null) {
            AuthRequest authRequest = new AuthRequest(basicCredentials[0], basicCredentials[1], null);
            AuthResult authResult = authProvider.authenticate(authRequest, buildContext(httpRequest));

            if (authResult.status() == AuthStatus.SUCCESS) {
                log.debug("管理面板 Basic 认证通过，用户: {}", authResult.userId());
                HttpAuthUtils.setSessionUser(httpRequest, authResult.userId());
                chain.doFilter(request, response);
                return;
            }
            log.warn("管理面板 Basic 认证失败: {}", authResult.message());
            resolveChallengeHandler().handleChallenge(httpRequest, httpResponse, authResult.message());
            return;
        }

        // 4. 无凭证信息，委托给挑战处理器
        resolveChallengeHandler().handleChallenge(httpRequest, httpResponse, null);
    }

    @Override
    public void destroy() {
        log.info("管理面板认证过滤器已销毁");
    }

    /**
     * 动态解析当前最高优先级的认证挑战处理器。
     * <p>
     * 优先从扩展点消费者获取，若无可用实现则使用默认 401 响应处理器作为兜底。
     *
     * @return 认证挑战处理器
     */
    private AuthChallengeHandler resolveChallengeHandler() {
        return challengeHandlerConsumer.get().orElse(fallbackHandler);
    }

    /**
     * 检查当前请求是否为公开端点（无需认证）。
     * <p>
     * 使用 AntPathMatcher 将请求路径与 {@link PanelWebProperties#getPublicEndpoints()}
     * 中配置的模式逐一匹配。
     *
     * @param request HTTP 请求
     * @return 是否为公开端点
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : properties.getPublicEndpoints()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建调用上下文。
     *
     * @param request HTTP 请求
     * @return 调用上下文
     */
    private InvocationContext buildContext(HttpServletRequest request) {
        return InvocationContext.builder()
                .channelId("HTTP")
                .attribute("clientIp", request.getRemoteAddr())
                .attribute("userAgent", request.getHeader("User-Agent"))
                .build();
    }
}
