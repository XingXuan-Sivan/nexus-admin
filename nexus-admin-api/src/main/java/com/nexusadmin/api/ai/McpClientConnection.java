package com.nexusadmin.api.ai;

import com.nexusadmin.core.extension.ExtensionPoint;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端连接扩展点，封装与远程 MCP 服务端的通信。
 *
 * <p>支持 HTTP JSON-RPC（默认）、stdio、SSE 等传输协议。
 * 插件可通过该扩展点提供自定义传输实现，平台默认提供 HTTP JSON-RPC 实现。</p>
 *
 * <p>典型用法：
 * <pre>{@code
 * McpClientConnection conn = ...;
 * conn.connect(new McpConnectionConfig("http://host/mcp", "", Map.of()));
 * for (McpRemoteTool tool : conn.listTools()) {
 *     McpToolResult result = conn.callTool(tool.name(), args);
 * }
 * conn.disconnect();
 * }</pre>
 */
public interface McpClientConnection extends ExtensionPoint {

    /**
     * 获取支持的传输协议名称。
     *
     * @return 协议名称，如 "http"、"stdio"、"sse"
     */
    String getProtocol();

    /**
     * 建立与远程 MCP 服务端的连接，执行 MCP initialize 握手。
     *
     * @param config 连接配置
     */
    void connect(McpConnectionConfig config);

    /**
     * 断开与远程 MCP 服务端的连接。
     */
    void disconnect();

    /**
     * 查询当前是否已建立连接。
     *
     * @return 如果已连接返回 true
     */
    boolean isConnected();

    /**
     * 列出远程 MCP 服务端提供的所有工具。
     *
     * @return 远程工具列表
     */
    List<McpRemoteTool> listTools();

    /**
     * 调用远程 MCP 工具。
     *
     * @param toolName  工具名称
     * @param arguments 调用参数
     * @return 调用结果
     */
    McpToolResult callTool(String toolName, Map<String, Object> arguments);

    /**
     * 获取远程 MCP 服务端信息。
     *
     * @return 服务端信息
     */
    McpServerInfo getServerInfo();

    /** 连接配置 */
    record McpConnectionConfig(String url, String authToken, Map<String, String> headers) {}

    /** 远程工具描述 */
    record McpRemoteTool(String name, String description, String inputSchema) {}

    /** 远程工具调用结果 */
    record McpToolResult(boolean success, String content, String error) {}

    /** 远程服务端信息 */
    record McpServerInfo(String name, String version) {}
}
