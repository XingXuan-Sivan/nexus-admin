package com.nexusadmin.api.ai;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;

import java.util.List;
import java.util.Set;

/**
 * MCP 远程工具桥接调度器。
 *
 * <p>监听 McpClientRegistry 的连接变更，将启用连接的远程工具
 * 通过 McpRemoteToolAdapter 注册到 AiToolRegistry。桥接后的远程工具
 * 与本地 Spring Bean AiTool 在 AiToolRegistry 中完全等价。</p>
 *
 * <p>同时维护桥接工具名称清单，为 MCP Server 分类暴露提供权威数据源。
 * McpController 通过 {@link #getAllBridgedToolNames()} 获取桥接工具集合，
 * 实现 tools/list 的 local/bridged/all 分类过滤。</p>
 *
 * <p><strong>桥接执行条件（全部满足才执行）：</strong>
 * <ul>
 *   <li>连接处于 CONNECTED 状态</li>
 *   <li>连接配置 enabled = true</li>
 *   <li>连接配置 bridgeEnabled = true</li>
 *   <li>远程工具列表非空</li>
 * </ul>
 */
public interface McpRemoteToolBridge {

    /**
     * 将指定连接的远程工具桥接到 AiToolRegistry。
     * 内部先调用 unbridge 清理旧桥接（防止重复注册），再拉取远程工具列表并注册。
     * 单个工具注册失败不影响同连接其他工具。
     *
     * @param connection 已建立连接的 MCP 客户端连接
     * @param info       连接配置信息（含 bridgeEnabled 等控制项）
     * @return 成功桥接的工具数量
     */
    int bridge(McpClientConnection connection, McpConnectionInfo info);

    /**
     * 从 AiToolRegistry 移除指定连接的所有桥接工具。
     *
     * @param connectionId 连接唯一标识
     * @return 移除的工具数量
     */
    int unbridge(String connectionId);

    /**
     * 刷新指定连接的工具桥接（断开旧桥接，重新拉取并注册）。
     *
     * @param connection 已建立连接的 MCP 客户端连接
     * @param info       连接配置信息
     * @return 刷新的工具数量
     */
    int refresh(McpClientConnection connection, McpConnectionInfo info);

    /**
     * 获取指定连接已桥接的工具名称列表。
     *
     * @param connectionId 连接唯一标识
     * @return 已桥接的工具名称列表，未桥接返回空列表
     */
    List<String> getBridgedTools(String connectionId);

    /**
     * 获取所有已桥接的工具名称集合。
     * 供 McpController 实现 tools/list 的 local/bridged/all 分类过滤。
     *
     * @return 所有已桥接工具的名称集合（以 mcp. 为前缀）
     */
    Set<String> getAllBridgedToolNames();

    /**
     * 判断指定连接是否可以执行桥接。
     *
     * @param info 连接配置信息
     * @return 如果满足桥接条件返回 true
     */
    boolean canBridge(McpConnectionInfo info);
}
