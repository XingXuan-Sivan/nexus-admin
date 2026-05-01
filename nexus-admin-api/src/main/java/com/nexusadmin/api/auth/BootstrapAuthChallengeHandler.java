package com.nexusadmin.api.auth;

import com.nexusadmin.api.extension.auth.AuthChallengeHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 引导认证挑战处理器，返回 HTML 登录页面。
 * <p>
 * 作为引导认证的前端门面，在没有任何其他认证实现时为用户提供可视化的登录入口。
 * 当存在其他认证提供者时，引导认证被自动禁用，此处理器也不再使用。
 * <p>
 * <strong>行为策略：</strong>
 * <ul>
 *   <li>GET /admin/login → 渲染登录页面，优先使用 Session 中的 flash 错误消息</li>
 *   <li>POST /admin/auth/login 失败 → 错误消息存入 Session 后重定向（PRG 模式，避免刷新重复提交表单）</li>
 *   <li>其他未认证请求 → 重定向到 /admin/login</li>
 * </ul>
 */
public class BootstrapAuthChallengeHandler implements AuthChallengeHandler {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAuthChallengeHandler.class);

    private static final String LOGIN_PATH = "/admin/login";
    private static final String LOGIN_POST_PATH = "/admin/auth/login";
    private static final String TEMPLATE_PATH = "templates/bootstrap-login.html";
    private static final String ERROR_PLACEHOLDER = "{{ERROR_MESSAGE}}";
    private static final String SESSION_FLASH_ERROR = "loginFlashError";

    /**
     * 登录页面 HTML 模板内容，启动时一次性加载。
     */
    private final String loginPageTemplate;

    /**
     * 构造引导认证挑战处理器。
     * <p>
     * 启动时从 classpath 加载登录页面模板。
     */
    public BootstrapAuthChallengeHandler() {
        this.loginPageTemplate = loadTemplate();
    }

    @Override
    public void handleChallenge(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // GET /admin/login → 渲染登录页面（优先使用 Session flash 消息）
        if ("GET".equals(method) && LOGIN_PATH.equals(path)) {
            String flashMessage = consumeFlashMessage(request);
            String displayMessage = flashMessage != null ? flashMessage : message;
            renderLoginPage(response, displayMessage);
            return;
        }

        // POST /admin/auth/login 登录失败 → 暂存错误消息后重定向（PRG 模式，避免刷新重复提交表单）
        if ("POST".equals(method) && LOGIN_POST_PATH.equals(path)) {
            if (message != null && !message.isEmpty()) {
                storeFlashMessage(request, message);
            }
            response.sendRedirect(LOGIN_PATH);
            return;
        }

        // 其他未认证请求 → 重定向到登录页面
        response.sendRedirect(LOGIN_PATH);
    }

    /**
     * 将错误消息暂存到 Session 中（flash 模式），供后续 GET 重定向使用。
     *
     * @param request HTTP 请求
     * @param message 错误消息
     */
    private void storeFlashMessage(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(SESSION_FLASH_ERROR, message);
    }

    /**
     * 从 Session 中取出并清除 flash 错误消息。
     *
     * @param request HTTP 请求
     * @return flash 错误消息，不存在则返回 null
     */
    private String consumeFlashMessage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object msg = session.getAttribute(SESSION_FLASH_ERROR);
        if (msg instanceof String s && !s.isEmpty()) {
            session.removeAttribute(SESSION_FLASH_ERROR);
            return s;
        }
        return null;
    }

    /**
     * 渲染登录页面。
     *
     * @param response     HTTP 响应
     * @param errorMessage 错误消息，为 null 或空表示无错误
     * @throws IOException 写入响应时发生 I/O 错误
     */
    private void renderLoginPage(HttpServletResponse response, String errorMessage) throws IOException {
        String html = buildLoginPage(errorMessage);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=UTF-8");
        response.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据模板构建登录页面 HTML。
     *
     * @param errorMessage 错误消息，为 null 或空表示无错误
     * @return 完整的 HTML 页面
     */
    private String buildLoginPage(String errorMessage) {
        String errorHtml = "";
        if (errorMessage != null && !errorMessage.isEmpty()) {
            errorHtml = "<div class=\"error-message\">" + escapeHtml(errorMessage) + "</div>";
        }
        return loginPageTemplate.replace(ERROR_PLACEHOLDER, errorHtml);
    }

    /**
     * 从 classpath 加载登录页面模板。
     *
     * @return 模板内容
     */
    private String loadTemplate() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                log.error("登录页面模板未找到: {}", TEMPLATE_PATH);
                return "<html><body><h1>登录页面模板加载失败</h1></body></html>";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载登录页面模板失败", e);
            return "<html><body><h1>登录页面模板加载失败</h1></body></html>";
        }
    }

    /**
     * 转义 HTML 特殊字符，防止 XSS。
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
