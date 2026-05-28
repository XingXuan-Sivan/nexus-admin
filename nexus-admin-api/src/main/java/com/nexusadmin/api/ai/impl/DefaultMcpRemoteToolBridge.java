package com.nexusadmin.api.ai.impl;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;
import com.nexusadmin.api.ai.AiToolRegistry;
import com.nexusadmin.api.ai.McpClientConnection;
import com.nexusadmin.api.ai.McpRemoteToolBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP 远程工具桥接调度器默认实现。
 *
 * <p>核心流程：
 * <ol>
 *   <li>连接注册成功 → 拉取远程工具列表</li>
 *   <li>对每个远程工具创建 McpRemoteToolAdapter</li>
 *   <li>注册到 AiToolRegistry，使平台 AI 可见</li>
 *   <li>连接注销时 → 从 AiToolRegistry 移除对应工具</li>
 * </ol>
 *
 * <p>维护每个连接已桥接的工具名称列表，支撑 MCP Server 按模式分类暴露。</p>
 */
public class DefaultMcpRemoteToolBridge implements McpRemoteToolBridge {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpRemoteToolBridge.class);

    /**
     * 维护每个连接已桥接的工具名称列表。
     * key: connectionId, value: 已注册到 AiToolRegistry 的 AiTool 名称列表。
     */
    private final ConcurrentHashMap<String, List<String>> bridgedRegistry
            = new ConcurrentHashMap<>();

    private final AiToolRegistry aiToolRegistry;

    public DefaultMcpRemoteToolBridge(AiToolRegistry aiToolRegistry) {
        this.aiToolRegistry = aiToolRegistry;
    }

    @Override
    public int bridge(McpClientConnection connection, McpConnectionInfo info) {
        if (!canBridge(info)) {
            log.debug("连接 {} 不满足桥接条件，跳过", info.getId());
            return 0;
        }
        if (!connection.isConnected()) {
            log.warn("连接 {} 未建立，无法桥接", info.getId());
            return 0;
        }

        unbridge(info.getId());

        List<McpClientConnection.McpRemoteTool> remoteTools;
        try {
            remoteTools = connection.listTools();
        } catch (Exception e) {
            log.error("获取远程工具列表失败: connectionId={}", info.getId(), e);
            return 0;
        }

        if (remoteTools.isEmpty()) {
            log.info("连接 {} 未提供任何远程工具", info.getId());
            return 0;
        }

        List<String> bridgedNames = new ArrayList<>();
        for (McpClientConnection.McpRemoteTool remoteTool : remoteTools) {
            try {
                McpRemoteToolAdapter adapter = new McpRemoteToolAdapter(
                        connection, remoteTool);
                aiToolRegistry.register(adapter);
                bridgedNames.add(adapter.getName());
            } catch (Exception e) {
                log.error("桥接远程工具失败: tool={}, connectionId={}",
                        remoteTool.name(), info.getId(), e);
            }
        }

        bridgedRegistry.put(info.getId(), Collections.unmodifiableList(bridgedNames));
        log.info("已桥接连接 {} 的 {} 个远程工具: {}",
                info.getId(),
                bridgedNames.size(),
                bridgedNames.stream().limit(5).collect(Collectors.joining(", ")));
        return bridgedNames.size();
    }

    @Override
    public int unbridge(String connectionId) {
        List<String> names = bridgedRegistry.remove(connectionId);
        if (names == null || names.isEmpty()) {
            return 0;
        }
        for (String toolName : names) {
            try {
                aiToolRegistry.unregister(toolName);
            } catch (Exception e) {
                log.error("注销桥接工具失败: toolName={}", toolName, e);
            }
        }
        log.info("已移除连接 {} 的 {} 个桥接工具", connectionId, names.size());
        return names.size();
    }

    @Override
    public int refresh(McpClientConnection connection, McpConnectionInfo info) {
        unbridge(info.getId());
        return bridge(connection, info);
    }

    @Override
    public List<String> getBridgedTools(String connectionId) {
        List<String> names = bridgedRegistry.get(connectionId);
        return names != null ? names : List.of();
    }

    @Override
    public Set<String> getAllBridgedToolNames() {
        Set<String> all = new HashSet<>();
        for (List<String> names : bridgedRegistry.values()) {
            all.addAll(names);
        }
        return Collections.unmodifiableSet(all);
    }

    @Override
    public boolean canBridge(McpConnectionInfo info) {
        return info != null && info.isEnabled() && info.isBridgeEnabled();
    }
}
