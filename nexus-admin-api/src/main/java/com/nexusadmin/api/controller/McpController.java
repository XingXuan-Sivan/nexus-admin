package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.ai.AiTool;
import com.nexusadmin.api.ai.AiToolRegistry;
import com.nexusadmin.api.ai.McpRemoteToolBridge;
import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MCP JSON-RPC 端点控制器。
 *
 * <p>手动实现 MCP 协议的 tools/list 和 tools/call 方法，
 * 兼容 LangChain4j 生态与标准 MCP 客户端。</p>
 *
 * <p>遵循 JSON-RPC 2.0 规范，所有 AiTool 自动暴露为 MCP Tool。
 * tools/list 支持可选的 mode 参数进行工具分类过滤：
 * <ul>
 *   <li>不传或 mode=all — 全部工具（本地 + 桥接）</li>
 *   <li>mode=local — 仅平台自带工具</li>
 *   <li>mode=bridged — 仅已桥接的远程 MCP 工具</li>
 * </ul>
 *
 * <p>由 {@code McpAutoConfig} 显式装配，不再通过组件扫描自动创建。</p>
 */
@RequestMapping("/admin/v1/mcp")
@Tag(name = "MCP 端点")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final AiToolRegistry toolRegistry;
    private final McpRemoteToolBridge bridge;

    public McpController(AiToolRegistry toolRegistry, McpRemoteToolBridge bridge) {
        this.toolRegistry = toolRegistry;
        this.bridge = bridge;
    }

    /**
     * MCP JSON-RPC 统一入口。
     *
     * <p>根据 JSON-RPC method 字段分发到 tools/list 或 tools/call。</p>
     *
     * @param requestBody JSON-RPC 请求体
     * @return JSON-RPC 响应体
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @RequirePermission("system.view")
    @Operation(summary = "MCP JSON-RPC 端点")
    public Map<String, Object> handleJsonRpc(@RequestBody Map<String, Object> requestBody) {
        String jsonrpc = (String) requestBody.getOrDefault("jsonrpc", "2.0");
        Object id = requestBody.get("id");
        String method = (String) requestBody.get("method");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) requestBody.getOrDefault("params", Map.of());

        try {
            Object result = switch (method) {
                case "tools/list" -> handleToolsList(params);
                case "tools/call" -> handleToolsCall(params);
                default -> throw new IllegalArgumentException("不支持的方法: " + method);
            };

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", jsonrpc);
            response.put("id", id);
            response.put("result", result);
            return response;
        } catch (Exception e) {
            log.error("MCP JSON-RPC 调用失败: method={}", method, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("jsonrpc", jsonrpc);
            error.put("id", id);
            Map<String, Object> errorObj = new LinkedHashMap<>();
            errorObj.put("code", -32000);
            errorObj.put("message", e.getMessage());
            error.put("error", errorObj);
            return error;
        }
    }

    /**
     * 列出可用工具，支持按模式分类过滤。
     *
     * @param params 请求参数，可选 mode 字段（all/local/bridged）
     * @return tools/list 响应
     */
    private Map<String, Object> handleToolsList(Map<String, Object> params) {
        String mode = params != null ? String.valueOf(params.getOrDefault("mode", "all")) : "all";

        List<AiTool> tools;
        switch (mode) {
            case "local" -> {
                Set<String> bridgedNames = bridge.getAllBridgedToolNames();
                tools = toolRegistry.listAll().stream()
                        .filter(t -> !bridgedNames.contains(t.getName()))
                        .toList();
            }
            case "bridged" -> {
                Set<String> bridgedNames = bridge.getAllBridgedToolNames();
                tools = bridgedNames.stream()
                        .map(toolRegistry::get)
                        .filter(t -> t != null)
                        .toList();
            }
            default -> tools = toolRegistry.listAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (AiTool tool : tools) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", tool.getName());
            entry.put("description", tool.getDescription());
            String inputSchemaStr = tool.getInputTypeSchema();
            if (inputSchemaStr != null && !inputSchemaStr.isEmpty()) {
                try {
                    entry.put("inputSchema", JSON.parse(inputSchemaStr));
                } catch (Exception e) {
                    entry.put("inputSchema", Map.of("type", "object", "properties", Map.of()));
                }
            } else {
                entry.put("inputSchema", Map.of("type", "object", "properties", Map.of()));
            }
            result.add(entry);
        }
        log.debug("MCP tools/list mode={} 返回 {} 个工具", mode, result.size());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tools", result);
        return response;
    }

    /**
     * 调用指定工具。
     *
     * @param params 包含 name 和 arguments 的参数
     * @return tools/call 响应
     */
    private Map<String, Object> handleToolsCall(Map<String, Object> params) {
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        if (toolName == null || toolName.isEmpty()) {
            throw new IllegalArgumentException("缺少工具名称");
        }

        AiTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("未找到工具: " + toolName);
        }

        String argumentsJson = JSON.toJSONString(arguments);
        String resultJson = tool.call(argumentsJson);

        log.debug("MCP tools/call: {} 执行完成", toolName);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", List.of(
                Map.of("type", "text", "text", resultJson)
        ));
        return response;
    }
}
