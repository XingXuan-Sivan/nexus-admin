package com.nexusadmin.api.ai;

import com.nexusadmin.api.domain.ai.McpConnectionInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP 客户端注册表，管理多个 MCP 客户端连接的生命周期。
 *
 * <p>聚合所有已注册的外部 MCP 服务连接，提供统一的远程工具查询与调用入口。
 * 启动时从持久化存储加载连接列表，运行时支持动态注册与注销。</p>
 */
public interface McpClientRegistry {

    /**
     * 注册并建立连接。
     *
     * @param info 连接配置信息
     */
    void register(McpConnectionInfo info);

    /**
     * 注销并断开连接。
     *
     * @param connectionId 连接唯一标识
     */
    void unregister(String connectionId);

    /**
     * 获取指定连接。
     *
     * @param connectionId 连接唯一标识
     * @return 连接实例，不存在时返回空
     */
    Optional<McpClientConnection> get(String connectionId);

    /**
     * 列出所有已注册连接。
     *
     * @return 连接配置信息列表
     */
    List<McpConnectionInfo> listAll();

    /**
     * 获取所有远程工具（聚合所有已连接服务端的工具列表）。
     *
     * @return 远程工具列表
     */
    List<McpClientConnection.McpRemoteTool> listAllRemoteTools();

    /**
     * 调用远程工具（自动定位所属连接）。
     *
     * @param toolName  工具名称
     * @param arguments 调用参数
     * @return 调用结果
     */
    McpClientConnection.McpToolResult callRemoteTool(
            String toolName, Map<String, Object> arguments);

    /**
     * 测试连接是否可用。
     *
     * @param info 连接配置信息
     * @return 如果连接成功返回 true
     */
    boolean testConnection(McpConnectionInfo info);
}
