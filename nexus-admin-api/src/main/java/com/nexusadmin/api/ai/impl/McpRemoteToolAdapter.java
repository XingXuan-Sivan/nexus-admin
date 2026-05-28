package com.nexusadmin.api.ai.impl;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.ai.AiTool;
import com.nexusadmin.api.ai.McpClientConnection;

import java.util.Map;

/**
 * 远程 MCP 工具适配器，将远程 MCP 工具桥接为平台 AiTool。
 *
 * <p>通过此适配器，平台 AI 可透明地调用外部 MCP 服务的工具，
 * 无需感知工具来源是本地方还是远程。</p>
 *
 * <p>远程工具名称自动添加 "mcp.{server}.{tool}" 前缀，避免与本地方工具冲突。</p>
 */
public class McpRemoteToolAdapter implements AiTool {

    private final McpClientConnection connection;
    private final McpClientConnection.McpRemoteTool remoteTool;
    private final String prefix;

    /**
     * 构造远程工具适配器。
     *
     * @param connection 远程 MCP 连接
     * @param remoteTool 远程工具描述
     */
    public McpRemoteToolAdapter(McpClientConnection connection,
                                 McpClientConnection.McpRemoteTool remoteTool) {
        this.connection = connection;
        this.remoteTool = remoteTool;
        McpClientConnection.McpServerInfo serverInfo = connection.getServerInfo();
        String serverName = serverInfo != null ? serverInfo.name() : "remote";
        this.prefix = "mcp." + serverName + ".";
    }

    @Override
    public String getName() {
        return prefix + remoteTool.name();
    }

    @Override
    public String getDescription() {
        return remoteTool.description();
    }

    @Override
    public String getInputTypeSchema() {
        return remoteTool.inputSchema();
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, InvocationContext context) {
        McpClientConnection.McpToolResult result = connection.callTool(
                remoteTool.name(), arguments);
        return new ToolResult(result.success(), result.content(), null);
    }
}
