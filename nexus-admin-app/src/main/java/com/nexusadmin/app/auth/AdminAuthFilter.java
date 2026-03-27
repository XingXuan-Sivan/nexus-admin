package com.nexusadmin.app.auth;

import com.nexusadmin.api.extension.auth.AuthProvider.AuthRequest;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthResult;
import com.nexusadmin.api.extension.auth.AuthProvider.AuthStatus;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 管理面板认证过滤器。
 * <p>
 * 拦截所有 /admin/** 请求，要求 Basic 认证。
 * <p>
 * <strong>工作流程：</strong>
 * <ul>
 *   <li>检查请求是否需要认证（/admin/** 路径）</li>
 *   <li>解析 Authorization 头获取 Basic 认证信息</li>
 *   <li>委托给 CompositeAuthProvider 进行认证</li>
 *   <li>认证成功则继续请求，失败则返回 401</li>
 * </ul>
 */
public class AdminAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String ADMIN_PATH_PREFIX = "/admin";

    private final CompositeAuthProvider authProvider;

    /**
     * 构造管理面板认证过滤器。
     *
     * @param authProvider 组合认证提供者
     */
    public AdminAuthFilter(CompositeAuthProvider authProvider) {
        this.authProvider = authProvider;
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

        // 只拦截 /admin/** 路径
        if (!requestPath.startsWith(ADMIN_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // 解析认证头
        String authHeader = httpRequest.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BASIC_PREFIX)) {
            sendAuthChallenge(httpResponse, "缺少认证信息");
            return;
        }

        // 解析 Basic 认证
        String[] credentials = parseBasicAuth(authHeader);
        if (credentials == null || credentials.length != 2) {
            sendAuthChallenge(httpResponse, "认证格式错误");
            return;
        }

        // 执行认证
        AuthRequest authRequest = new AuthRequest(credentials[0], credentials[1], null);
        AuthResult authResult = authProvider.authenticate(authRequest, null);

        if (authResult.status() == AuthStatus.SUCCESS) {
            log.debug("管理面板认证成功，用户: {}", authResult.userId());
            chain.doFilter(request, response);
        } else {
            log.warn("管理面板认证失败: {}", authResult.message());
            sendAuthChallenge(httpResponse, authResult.message());
        }
    }

    @Override
    public void destroy() {
        log.info("管理面板认证过滤器已销毁");
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
     * 发送认证挑战响应。
     *
     * @param response HTTP 响应
     * @param message  错误消息
     */
    private void sendAuthChallenge(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Admin Panel\"");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
