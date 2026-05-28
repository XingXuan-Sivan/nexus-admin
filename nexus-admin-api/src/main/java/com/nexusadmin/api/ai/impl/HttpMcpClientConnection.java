package com.nexusadmin.api.ai.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nexusadmin.api.ai.McpClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端 HTTP JSON-RPC 默认实现。
 *
 * <p>遵循 MCP 协议规范，通过 HTTP POST 发送 JSON-RPC 2.0 请求与远程 MCP 服务端通信。
 * 支持 tools/list 与 tools/call 两个核心方法。</p>
 */
public class HttpMcpClientConnection implements McpClientConnection {

    private static final Logger log = LoggerFactory.getLogger(HttpMcpClientConnection.class);

    private static final String PROTOCOL = "http";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private volatile McpConnectionConfig config;
    private volatile boolean connected;
    private volatile McpServerInfo serverInfo;

    private final HttpClient httpClient;

    public HttpMcpClientConnection() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public void connect(McpConnectionConfig config) {
        this.config = config;
        try {
            Map<String, Object> request = buildJsonRpcRequest("initialize", Map.of());
            Map<String, Object> response = sendJsonRpc(request);
            if (response.containsKey("error")) {
                log.warn("MCP 连接初始化返回错误: {}", response.get("error"));
                this.connected = false;
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            if (result != null) {
                String serverName = String.valueOf(result.getOrDefault("serverInfo",
                        Map.of("name", "unknown", "version", "0.0.0")));
                this.serverInfo = new McpServerInfo(
                        extractServerName(result),
                        String.valueOf(result.getOrDefault("protocolVersion", "unknown"))
                );
            }
            this.connected = true;
            log.info("已连接 MCP 服务端: {} ({} {})", config.url(),
                    serverInfo != null ? serverInfo.name() : "unknown",
                    serverInfo != null ? serverInfo.version() : "");
        } catch (Exception e) {
            log.error("MCP 连接失败: url={}", config.url(), e);
            this.connected = false;
        }
    }

    @Override
    public void disconnect() {
        this.connected = false;
        this.serverInfo = null;
        log.info("已断开 MCP 连接");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<McpRemoteTool> listTools() {
        if (!connected) {
            log.warn("MCP 连接未建立，无法列出工具");
            return List.of();
        }
        Map<String, Object> request = buildJsonRpcRequest("tools/list", Map.of());
        try {
            Map<String, Object> response = sendJsonRpc(request);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            if (result == null) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
            if (tools == null) {
                return List.of();
            }
            List<McpRemoteTool> remoteTools = new ArrayList<>();
            for (Map<String, Object> tool : tools) {
                String name = String.valueOf(tool.get("name"));
                String desc = String.valueOf(tool.getOrDefault("description", ""));
                String schema = JSON.toJSONString(tool.get("inputSchema"));
                remoteTools.add(new McpRemoteTool(name, desc, schema));
            }
            return remoteTools;
        } catch (Exception e) {
            log.error("列出远程工具失败", e);
            return List.of();
        }
    }

    @Override
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        if (!connected) {
            return new McpToolResult(false, "", "MCP 连接未建立");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : Map.of());

        Map<String, Object> request = buildJsonRpcRequest("tools/call", params);
        try {
            Map<String, Object> response = sendJsonRpc(request);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            if (result == null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                String errMsg = error != null ? String.valueOf(error.get("message")) : "未知错误";
                return new McpToolResult(false, "", errMsg);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content != null && !content.isEmpty()) {
                String text = String.valueOf(content.get(0).getOrDefault("text", ""));
                return new McpToolResult(true, text, null);
            }
            return new McpToolResult(true, JSON.toJSONString(result), null);
        } catch (Exception e) {
            log.error("调用远程工具失败: toolName={}", toolName, e);
            return new McpToolResult(false, "", e.getMessage());
        }
    }

    @Override
    public McpServerInfo getServerInfo() {
        return serverInfo;
    }

    private Map<String, Object> buildJsonRpcRequest(String method, Map<String, Object> params) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", System.currentTimeMillis());
        request.put("method", method);
        request.put("params", params);
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendJsonRpc(Map<String, Object> request) throws Exception {
        String body = JSON.toJSONString(request);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.url()))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (config.authToken() != null && !config.authToken().isEmpty()) {
            builder.header("Authorization", "Bearer " + config.authToken());
        }
        if (config.headers() != null) {
            config.headers().forEach(builder::header);
        }

        HttpResponse<String> httpResponse = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() >= 400) {
            throw new RuntimeException("HTTP 请求失败: " + httpResponse.statusCode());
        }

        JSONObject json = JSON.parseObject(httpResponse.body());
        return (Map<String, Object>) (Map) json;
    }

    private String extractServerName(Map<String, Object> result) {
        Object serverInfoObj = result.get("serverInfo");
        if (serverInfoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) serverInfoObj;
            return String.valueOf(info.getOrDefault("name", "unknown"));
        }
        return String.valueOf(result.getOrDefault("serverInfo", "unknown"));
    }
}
