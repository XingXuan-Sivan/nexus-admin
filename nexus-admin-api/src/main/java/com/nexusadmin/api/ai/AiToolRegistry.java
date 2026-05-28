package com.nexusadmin.api.ai;

import java.util.List;

/**
 * AI 工具注册表，聚合 Spring Bean 发现与运行时注册，作为工具的唯一权威来源。
 *
 * <p>LangChain4j 工具调用与 MCP Server 通过此注册表获取所有可用工具。
 * Spring 管理的 AiTool Bean 由 DefaultAiToolRegistry 自动收集，
 * 插件也可在运行时调用 {@link #register(AiTool)} 动态注册。</p>
 */
public interface AiToolRegistry {

    /**
     * 注册工具（运行时动态注册，用于非 Spring Bean 场景）。
     *
     * @param tool 工具实例
     */
    void register(AiTool tool);

    /**
     * 注销工具。
     *
     * @param toolName 工具名称
     */
    void unregister(String toolName);

    /**
     * 获取所有已注册工具（聚合 Spring Bean + 运行时注册，去重）。
     *
     * @return 所有已注册工具列表
     */
    List<AiTool> listAll();

    /**
     * 按名称查找工具。
     *
     * @param toolName 工具名称
     * @return 工具实例，不存在返回 null
     */
    AiTool get(String toolName);
}
