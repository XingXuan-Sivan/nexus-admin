package com.nexusadmin.api.ai.impl;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;
import com.nexusadmin.api.ai.McpClientConnection;
import com.nexusadmin.api.ai.McpClientRegistry;
import com.nexusadmin.api.ai.McpRemoteToolBridge;
import com.nexusadmin.core.facade.ConfigFacade;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 客户端注册表默认实现。
 *
 * <p>管理多个 MCP 客户端连接的生命周期，支持：
 * <ul>
 *   <li>从配置中心加载已保存的连接列表</li>
 *   <li>运行时动态注册/注销连接</li>
 *   <li>连接配置变更后自动持久化</li>
 *   <li>聚合所有连接的远程工具列表</li>
 * </ul>
 */
public class DefaultMcpClientRegistry implements McpClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClientRegistry.class);

    private static final String SCOPE = "mcp-clients";
    private static final String KEY = "connections";

    private final ConcurrentHashMap<String, McpClientConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, McpConnectionInfo> connectionInfos = new ConcurrentHashMap<>();

    private final ConfigFacade configFacade;
    private final McpRemoteToolBridge bridge;

    public DefaultMcpClientRegistry(ConfigFacade configFacade,
                                     McpRemoteToolBridge bridge) {
        this.configFacade = configFacade;
        this.bridge = bridge;
        loadFromStorage();
    }

    @Override
    public void register(McpConnectionInfo info) {
        if (info.getId() == null || info.getId().isEmpty()) {
            throw new IllegalArgumentException("连接标识不能为空");
        }
        String protocol = info.getProtocol() != null ? info.getProtocol() : "http";
        if (!"http".equals(protocol)) {
            log.warn("当前仅支持 HTTP 协议，收到: {}", protocol);
        }
        McpClientConnection connection = new HttpMcpClientConnection();
        McpClientConnection.McpConnectionConfig config = new McpClientConnection.McpConnectionConfig(
                info.getUrl(),
                info.getAuthToken(),
                Map.of()
        );
        connection.connect(config);
        connections.put(info.getId(), connection);
        connectionInfos.put(info.getId(), info);
        info.setStatus(connection.isConnected() ? "CONNECTED" : "ERROR");

        if (connection.isConnected() && bridge.canBridge(info)) {
            int count = bridge.bridge(connection, info);
            log.info("连接 {} 已自动桥接 {} 个远程工具到 AiToolRegistry", info.getId(), count);
        }

        persistToStorage();
        log.info("已注册 MCP 客户端连接: id={}, name={}, status={}",
                info.getId(), info.getName(), info.getStatus());
    }

    @Override
    public void unregister(String connectionId) {
        bridge.unbridge(connectionId);

        McpClientConnection connection = connections.remove(connectionId);
        if (connection != null) {
            connection.disconnect();
        }
        connectionInfos.remove(connectionId);
        persistToStorage();
        log.info("已注销 MCP 客户端连接: id={}", connectionId);
    }

    @Override
    public Optional<McpClientConnection> get(String connectionId) {
        return Optional.ofNullable(connections.get(connectionId));
    }

    @Override
    public List<McpConnectionInfo> listAll() {
        List<McpConnectionInfo> result = new ArrayList<>(connectionInfos.values());
        for (McpConnectionInfo info : result) {
            McpClientConnection conn = connections.get(info.getId());
            if (conn != null) {
                info.setStatus(conn.isConnected() ? "CONNECTED" : "DISCONNECTED");
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<McpClientConnection.McpRemoteTool> listAllRemoteTools() {
        List<McpClientConnection.McpRemoteTool> allTools = new ArrayList<>();
        for (Map.Entry<String, McpClientConnection> entry : connections.entrySet()) {
            if (entry.getValue().isConnected()) {
                List<McpClientConnection.McpRemoteTool> tools = entry.getValue().listTools();
                allTools.addAll(tools);
            }
        }
        return allTools;
    }

    @Override
    public McpClientConnection.McpToolResult callRemoteTool(
            String toolName, Map<String, Object> arguments) {
        for (McpClientConnection conn : connections.values()) {
            if (!conn.isConnected()) {
                continue;
            }
            List<McpClientConnection.McpRemoteTool> tools = conn.listTools();
            boolean found = tools.stream().anyMatch(t -> t.name().equals(toolName));
            if (found) {
                return conn.callTool(toolName, arguments);
            }
        }
        return new McpClientConnection.McpToolResult(false, "", "未找到工具: " + toolName);
    }

    @Override
    public boolean testConnection(McpConnectionInfo info) {
        McpClientConnection testConn = new HttpMcpClientConnection();
        try {
            testConn.connect(new McpClientConnection.McpConnectionConfig(
                    info.getUrl(), info.getAuthToken(), Map.of()));
            boolean result = testConn.isConnected();
            testConn.disconnect();
            return result;
        } catch (Exception e) {
            log.warn("MCP 连接测试失败: url={}", info.getUrl(), e);
            return false;
        }
    }

    private void loadFromStorage() {
        try {
            Optional<String> stored = configFacade.get(SCOPE, KEY);
            if (stored.isPresent()) {
                List<McpConnectionInfo> infos = JSON.parseArray(stored.get(), McpConnectionInfo.class);
                if (infos != null) {
                    for (McpConnectionInfo info : infos) {
                        if (info.isEnabled()) {
                            register(info);
                        } else {
                            connectionInfos.put(info.getId(), info);
                            info.setStatus("DISABLED");
                        }
                    }
                    log.info("已从配置中心加载 {} 个 MCP 客户端连接", infos.size());
                }
            }
        } catch (Exception e) {
            log.warn("加载 MCP 客户端连接配置失败", e);
        }
    }

    private void persistToStorage() {
        try {
            List<McpConnectionInfo> infos = new ArrayList<>(connectionInfos.values());
            configFacade.set(SCOPE, KEY, JSON.toJSONString(infos));
        } catch (Exception e) {
            log.error("持久化 MCP 客户端连接配置失败", e);
        }
    }
}
