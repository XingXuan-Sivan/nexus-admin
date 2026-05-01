package com.nexusadmin.api.auth;

import com.nexusadmin.api.extension.auth.AuthChallengeHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 默认认证挑战处理器，返回标准 HTTP 401 响应。
 * <p>
 * 适用于 API 客户端场景，返回 401 状态码、WWW-Authenticate 头和 JSON 错误消息。
 * 当引导认证被禁用（存在其他认证提供者）时使用此处理器。
 */
public class DefaultAuthChallengeHandler implements AuthChallengeHandler {

    @Override
    public void handleChallenge(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Admin Panel\"");
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"error\":\"" + escapeJson(message) + "\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
