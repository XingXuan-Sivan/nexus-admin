package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;
import com.nexusadmin.api.ai.McpClientConnection;
import com.nexusadmin.api.ai.McpClientRegistry;
import com.nexusadmin.api.ai.McpRemoteToolBridge;
import com.nexusadmin.api.service.McpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端业务服务默认实现。
 *
 * <p>封装对 McpClientRegistry 的管理操作，处理 CRUD 流程中的：
 * <ul>
 *   <li>连接标识生成</li>
 *   <li>协议默认值填充</li>
 *   <li>启用/禁用切换时的连接注册/注销</li>
 *   <li>桥接刷新与查询</li>
 * </ul>
 */
public class DefaultMcpClientService implements McpClientService {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClientService.class);

    private final McpClientRegistry registry;
    private final McpRemoteToolBridge bridge;

    public DefaultMcpClientService(McpClientRegistry registry, McpRemoteToolBridge bridge) {
        this.registry = registry;
        this.bridge = bridge;
    }

    @Override
    public List<McpConnectionInfo> list() {
        return registry.listAll();
    }

    @Override
    public McpConnectionInfo get(String id) {
        List<McpConnectionInfo> all = registry.listAll();
        return all.stream()
                .filter(info -> info.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public McpConnectionInfo create(McpConnectionInfo info) {
        if (info.getId() == null || info.getId().isEmpty()) {
            info.setId("mcp-" + System.currentTimeMillis());
        }
        if (info.getProtocol() == null || info.getProtocol().isEmpty()) {
            info.setProtocol("http");
        }
        if (info.isEnabled()) {
            registry.register(info);
        }
        log.info("已创建 MCP 客户端连接: id={}, name={}", info.getId(), info.getName());
        return info;
    }

    @Override
    public McpConnectionInfo update(String id, McpConnectionInfo info) {
        registry.unregister(id);
        info.setId(id);
        if (info.getProtocol() == null || info.getProtocol().isEmpty()) {
            info.setProtocol("http");
        }
        if (info.isEnabled()) {
            registry.register(info);
        }
        log.info("已更新 MCP 客户端连接: id={}, name={}", id, info.getName());
        return info;
    }

    @Override
    public void delete(String id) {
        registry.unregister(id);
        log.info("已删除 MCP 客户端连接: id={}", id);
    }

    @Override
    public boolean testConnection(McpConnectionInfo info) {
        return registry.testConnection(info);
    }

    @Override
    public List<McpClientConnection.McpRemoteTool> listRemoteTools(String connectionId) {
        return registry.get(connectionId)
                .filter(McpClientConnection::isConnected)
                .map(McpClientConnection::listTools)
                .orElse(List.of());
    }

    @Override
    public List<McpClientConnection.McpRemoteTool> listAllRemoteTools() {
        return registry.listAllRemoteTools();
    }

    @Override
    public McpClientConnection.McpToolResult callRemoteTool(
            String toolName, Map<String, Object> arguments) {
        return registry.callRemoteTool(toolName, arguments);
    }

    @Override
    public int refreshBridge(String connectionId) {
        McpClientConnection connection = registry.get(connectionId).orElse(null);
        if (connection == null || !connection.isConnected()) {
            log.warn("无法刷新桥接：连接 {} 未建立或不存在", connectionId);
            return 0;
        }
        McpConnectionInfo info = registry.listAll().stream()
                .filter(i -> i.getId().equals(connectionId))
                .findFirst()
                .orElse(null);
        if (info == null) {
            return 0;
        }
        int count = bridge.refresh(connection, info);
        log.info("已刷新连接 {} 的桥接，共 {} 个工具", connectionId, count);
        return count;
    }

    @Override
    public List<String> getBridgedTools(String connectionId) {
        return bridge.getBridgedTools(connectionId);
    }
}
