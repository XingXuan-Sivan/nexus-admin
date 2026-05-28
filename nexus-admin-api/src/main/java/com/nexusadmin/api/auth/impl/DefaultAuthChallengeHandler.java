package com.nexusadmin.api.auth.impl;

import com.nexusadmin.api.auth.AuthChallengeHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 默认认证挑战处理器，返回标准 HTTP 401 JSON 响应。
 * <p>
 * 适用于 API 客户端场景。不设置 WWW-Authenticate 响应头，
 * 避免触发浏览器原生 Basic 认证弹窗。认证失败的详细原因通过 JSON 响应体返回。
 */
public class DefaultAuthChallengeHandler implements AuthChallengeHandler {

    @Override
    public void handleChallenge(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
