package com.nexusadmin.api.auth;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.extension.auth.AuthChallengeHandler;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthStatus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 管理面板认证过滤器。
 * <p>
 * 拦截所有 /admin/v1/** 请求，执行认证检查。
 * <p>
 * <strong>凭证来源优先级：</strong>
 * <ol>
 *   <li>Bearer Token（Authorization: Bearer xxx）</li>
 *   <li>Session 中已认证的用户信息</li>
 *   <li>Basic 认证头（Authorization: Basic xxx）</li>
 *   <li>表单提交参数（username / password）</li>
 * </ol>
 * <p>
 * <strong>公开端点（无需认证）：</strong>
 * <ul>
 *   <li>POST /admin/v1/auth/login</li>
 *   <li>/v3/api-docs/**</li>
 *   <li>/doc.html</li>
 *   <li>/webjars/**</li>
 * </ul>
 * <p>
 * <strong>认证失败处理：</strong>
 * 委托给 {@link AuthChallengeHandler}，由其决定返回 401 JSON 响应还是 HTML 登录页面，
 * 保持过滤器本身不感知任何前端实现细节。
 */
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String ADMIN_PATH_PREFIX = "/admin/v1";
    private static final String LOGIN_PATH = "/admin/v1/auth/login";
    private static final String SESSION_ATTR_USER = "authenticatedUser";

    private final CompositeAuthProvider authProvider;
    private final ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer;
    private final AuthChallengeHandler fallbackHandler;

    /**
     * 构造管理面板认证过滤器。
     *
     * @param authProvider            组合认证提供者
     * @param challengeHandlerConsumer 认证挑战处理器扩展点消费者
     */
    public AuthFilter(CompositeAuthProvider authProvider,
                      ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer) {
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

        // 只拦截 /admin/v1/** 路径
        if (!requestPath.startsWith(ADMIN_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查公开端点白名单
        if (isPublicEndpoint(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 检查 Session 中是否已认证
        String sessionUser = getSessionUser(httpRequest);
        if (sessionUser != null) {
            log.debug("管理面板会话认证通过，用户: {}", sessionUser);
            chain.doFilter(request, response);
            return;
        }

        // 2. 尝试从请求中提取凭证（按优先级）

        // 2.1 Bearer Token
        String bearerToken = extractBearerToken(httpRequest);
        if (bearerToken != null) {
            AuthResult authResult = authProvider.validateToken(bearerToken, buildContext(httpRequest));
            if (authResult.status() == AuthStatus.SUCCESS) {
                log.debug("管理面板 Bearer Token 认证通过，用户: {}", authResult.userId());
                setSessionUser(httpRequest, authResult.userId());
                chain.doFilter(request, response);
                return;
            } else {
                log.warn("管理面板 Bearer Token 认证失败: {}", authResult.message());
                resolveChallengeHandler().handleChallenge(httpRequest, httpResponse, authResult.message());
                return;
            }
        }

        // 2.2 Basic Auth 和表单参数
        Credentials credentials = extractCredentials(httpRequest);

        if (credentials != null) {
            // 3. 执行认证
            AuthRequest authRequest = new AuthRequest(credentials.username, credentials.password, null);
            AuthResult authResult = authProvider.authenticate(authRequest, buildContext(httpRequest));

            if (authResult.status() == AuthStatus.SUCCESS) {
                log.debug("管理面板认证成功，用户: {}", authResult.userId());
                setSessionUser(httpRequest, authResult.userId());

                // 表单登录成功后重定向到管理面板首页
                if (credentials.isFormPost) {
                    String redirectUrl = getRedirectUrl(httpRequest);
                    httpResponse.sendRedirect(redirectUrl);
                    return;
                }
                chain.doFilter(request, response);
            } else {
                log.warn("管理面板认证失败: {}", authResult.message());
                resolveChallengeHandler().handleChallenge(httpRequest, httpResponse, authResult.message());
            }
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
     *
     * @param request HTTP 请求
     * @return 是否为公开端点
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && LOGIN_PATH.equals(path)) {
            return true;
        }
        if (path.startsWith("/v3/api-docs/")) {
            return true;
        }
        if ("/doc.html".equals(path)) {
            return true;
        }
        if (path.startsWith("/webjars/")) {
            return true;
        }
        return false;
    }

    /**
     * 从请求中提取认证凭证。
     * <p>
     * 按优先级依次尝试 Basic 认证头和表单参数。
     *
     * @param request HTTP 请求
     * @return 凭证信息，无法提取时返回 null
     */
    private Credentials extractCredentials(HttpServletRequest request) {
        // 尝试 Basic 认证头
        Credentials basicCreds = extractBasicCredentials(request);
        if (basicCreds != null) {
            return basicCreds;
        }

        // 尝试表单参数（仅对 POST /admin/v1/auth/login 生效）
        return extractFormCredentials(request);
    }

    /**
     * 从 Bearer Token 认证头提取 Token。
     *
     * @param request HTTP 请求
     * @return Bearer Token，不存在或格式错误时返回 null
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 从 Basic 认证头提取凭证。
     *
     * @param request HTTP 请求
     * @return 凭证信息，解析失败时返回 null
     */
    private Credentials extractBasicCredentials(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BASIC_PREFIX)) {
            return null;
        }

        String[] credentials = parseBasicAuth(authHeader);
        if (credentials == null || credentials.length != 2) {
            return null;
        }
        return new Credentials(credentials[0], credentials[1], false);
    }

    /**
     * 从表单参数提取凭证。
     * <p>
     * 仅对 POST /admin/v1/auth/login 请求生效，避免意外读取其他 POST 请求的参数。
     *
     * @param request HTTP 请求
     * @return 凭证信息，无法提取时返回 null
     */
    private Credentials extractFormCredentials(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        if (!LOGIN_PATH.equals(request.getRequestURI())) {
            return null;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return null;
        }
        return new Credentials(username, password, true);
    }

    /**
     * 解析 Basic 认证头。
     *
     * @param authHeader 认证头
     * @return [username, password]，解析失败返回 null
     */
    private String[] parseBasicAuth(String authHeader) {
        try {
            String base64Credentials = authHeader.substring(BASIC_PREFIX.length());
            String credentials = new String(
                    Base64.getDecoder().decode(base64Credentials),
                    StandardCharsets.UTF_8
            );
            int colonIndex = credentials.indexOf(':');
            if (colonIndex <= 0) {
                return null;
            }
            String username = credentials.substring(0, colonIndex);
            String password = credentials.substring(colonIndex + 1);
            return new String[]{username, password};
        } catch (Exception e) {
            log.warn("解析 Basic 认证头失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Session 中获取已认证用户。
     *
     * @param request HTTP 请求
     * @return 已认证用户标识，未认证返回 null
     */
    private String getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(SESSION_ATTR_USER);
        return user instanceof String s ? s : null;
    }

    /**
     * 将认证用户信息存入 Session。
     *
     * @param request HTTP 请求
     * @param userId  用户标识
     */
    private void setSessionUser(HttpServletRequest request, String userId) {
        request.getSession(true).setAttribute(SESSION_ATTR_USER, userId);
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

    /**
     * 获取登录成功后的重定向地址。
     * <p>
     * 优先使用 Session 中保存的原始请求地址，否则重定向到 /admin/v1。
     *
     * @param request HTTP 请求
     * @return 重定向地址
     */
    private String getRedirectUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object originalUrl = session.getAttribute("requestedUrl");
            if (originalUrl instanceof String url && url.startsWith("/admin")) {
                session.removeAttribute("requestedUrl");
                return url;
            }
        }
        return "/admin/v1";
    }

    /**
     * 认证凭证信息。
     */
    private record Credentials(String username, String password, boolean isFormPost) {
    }
}
