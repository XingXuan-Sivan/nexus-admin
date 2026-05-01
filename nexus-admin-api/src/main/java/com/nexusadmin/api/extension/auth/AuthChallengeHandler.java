package com.nexusadmin.api.extension.auth;

import com.nexusadmin.core.extension.ExtensionPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 认证挑战处理器，定义认证失败时的响应策略。
 * <p>
 * 不同的认证场景需要不同的失败响应：
 * <ul>
 *   <li>默认场景：返回 401 状态码和 JSON 错误消息</li>
 *   <li>引导认证场景：返回 HTML 登录页面或重定向到登录页</li>
 * </ul>
 * <p>
 * AuthFilter 在认证失败时将响应委托给此接口，从而将认证失败的处理策略
 * 与过滤器本身解耦，保持过滤器的通用性。
 */
public interface AuthChallengeHandler extends ExtensionPoint {

    /**
     * 处理认证挑战，向客户端返回适当的未认证响应。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param message  错误消息
     * @throws IOException 写入响应时发生 I/O 错误
     */
    void handleChallenge(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException;
}
