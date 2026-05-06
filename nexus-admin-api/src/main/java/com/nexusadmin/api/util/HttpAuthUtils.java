package com.nexusadmin.api.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP 认证工具类，提供 Token 提取、Session 操作等通用静态方法。
 * <p>
 * 将 Bearer Token 解析、Basic 认证头解析、Session 用户读写等
 * 跨组件复用的通用逻辑收敛至此，消除 {@code AuthFilter}、
 * {@code AuthController}、{@code PermissionInterceptor} 中的重复代码。
 * <p>
 * 该类为纯静态工具类，禁止实例化，不依赖 Spring 容器。
 */
public final class HttpAuthUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpAuthUtils.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String SESSION_ATTR_USER = "authenticatedUser";

    private HttpAuthUtils() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 从请求中提取 Bearer Token。
     *
     * @param request HTTP 请求
     * @return Bearer Token，不存在或格式错误时返回 null
     */
    public static String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 解析 Basic 认证头，提取用户名和密码。
     *
     * @param authHeader 完整的 Authorization 头值（需以 "Basic " 开头）
     * @return 包含 [username, password] 的数组，解析失败返回 null
     */
    public static String[] parseBasicAuth(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BASIC_PREFIX)) {
            return null;
        }
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
     * 从 Session 中获取已认证的用户标识。
     *
     * @param request HTTP 请求
     * @return 已认证用户标识，未认证返回 null
     */
    public static String getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(SESSION_ATTR_USER);
        return user instanceof String s ? s : null;
    }

    /**
     * 将已认证的用户标识存入 Session。
     *
     * @param request HTTP 请求
     * @param userId  用户标识
     */
    public static void setSessionUser(HttpServletRequest request, String userId) {
        request.getSession(true).setAttribute(SESSION_ATTR_USER, userId);
    }
}
