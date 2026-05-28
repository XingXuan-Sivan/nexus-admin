package com.nexusadmin.api.service;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;
import com.nexusadmin.api.ai.McpClientConnection;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端业务服务，封装对 MCP 客户端注册表的管理操作。
 *
 * <p>提供连接配置的 CRUD 操作、连接测试、远程工具浏览与调用能力。
 * 遵循平台统一的 CRUD 方法命名规范（list/get/create/update/delete）。</p>
 */
public interface McpClientService {

    /**
     * 获取所有 MCP 客户端连接配置列表。
     *
     * @return 连接配置列表
     */
    List<McpConnectionInfo> list();

    /**
     * 获取指定连接配置详情。
     *
     * @param id 连接唯一标识
     * @return 连接配置，不存在时返回 null
     */
    McpConnectionInfo get(String id);

    /**
     * 创建新的 MCP 客户端连接。
     *
     * @param info 连接配置信息
     * @return 创建后的连接配置
     */
    McpConnectionInfo create(McpConnectionInfo info);

    /**
     * 更新已有的 MCP 客户端连接配置。
     *
     * @param id   连接唯一标识
     * @param info 新的连接配置信息
     * @return 更新后的连接配置
     */
    McpConnectionInfo update(String id, McpConnectionInfo info);

    /**
     * 删除指定的 MCP 客户端连接。
     *
     * @param id 连接唯一标识
     */
    void delete(String id);

    /**
     * 测试指定连接的可用性。
     *
     * @param info 连接配置信息
     * @return 如果连接成功返回 true
     */
    boolean testConnection(McpConnectionInfo info);

    /**
     * 列出指定连接的所有远程工具。
     *
     * @param connectionId 连接唯一标识
     * @return 远程工具列表
     */
    List<McpClientConnection.McpRemoteTool> listRemoteTools(String connectionId);

    /**
     * 聚合所有连接的远程工具列表。
     *
     * @return 远程工具列表
     */
    List<McpClientConnection.McpRemoteTool> listAllRemoteTools();

    /**
     * 调用指定远程工具。
     *
     * @param toolName  工具名称
     * @param arguments 调用参数
     * @return 调用结果
     */
    McpClientConnection.McpToolResult callRemoteTool(
            String toolName, Map<String, Object> arguments);

    /**
     * 刷新指定连接的远程工具桥接。
     *
     * @param connectionId 连接唯一标识
     * @return 刷新后的工具数量
     */
    int refreshBridge(String connectionId);

    /**
     * 获取指定连接已桥接的工具名称列表。
     *
     * @param connectionId 连接唯一标识
     * @return 已桥接的工具名称列表
     */
    List<String> getBridgedTools(String connectionId);
}
