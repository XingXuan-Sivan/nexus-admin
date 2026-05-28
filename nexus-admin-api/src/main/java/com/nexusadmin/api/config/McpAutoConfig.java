package com.nexusadmin.api.config;

import com.nexusadmin.api.ai.impl.HttpMcpClientConnection;
import com.nexusadmin.api.controller.McpController;
import com.nexusadmin.api.ai.AiToolRegistry;
import com.nexusadmin.api.ai.McpClientConnection;
import com.nexusadmin.api.ai.McpClientRegistry;
import com.nexusadmin.api.ai.McpRemoteToolBridge;
import com.nexusadmin.api.ai.impl.AIToolProvider;
import com.nexusadmin.api.ai.impl.DefaultMcpClientRegistry;
import com.nexusadmin.api.ai.impl.DefaultMcpRemoteToolBridge;
import com.nexusadmin.api.service.McpClientService;
import com.nexusadmin.api.service.impl.DefaultMcpClientService;
import com.nexusadmin.core.facade.ConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 统一装配配置。
 *
 * <p>统一装配全部 MCP 相关 Bean，包括 MCP 服务端端点、MCP 客户端连接管理能力、
 * 远程工具桥接调度器以及 LangChain4j 集成。
 * 所有 Bean 带 {@code @ConditionalOnMissingBean} 保护，允许插件通过声明同类型 Bean 覆盖默认实现。</p>
 *
 * <p><strong>职责划分：</strong>
 * <ul>
 *   <li>{@code AiAutoConfig} — 仅负责 AI 核心能力（AiToolRegistry、AiProvider、ChatLanguageModel 桥接）</li>
 *   <li>{@code McpAutoConfig} — 全部 MCP 相关装配（Server + Client + Bridge + LangChain4j 集成）</li>
 * </ul>
 */
@Configuration
public class McpAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(McpAutoConfig.class);

    /**
     * MCP 服务端端点控制器。
     *
     * <p>显式装配 McpController，注入 AiToolRegistry 和 McpRemoteToolBridge，
     * 实现 tools/list 的 mode=all/local/bridged 分类过滤。</p>
     *
     * @param toolRegistry AI 工具注册表
     * @param bridge       MCP 远程工具桥接调度器（提供桥接工具名集合）
     * @return McpController 实例
     */
    @Bean
    @ConditionalOnMissingBean(McpController.class)
    public McpController mcpController(AiToolRegistry toolRegistry,
                                        McpRemoteToolBridge bridge) {
        log.info("已装配 McpController（MCP 服务端端点，支持工具分类过滤）");
        return new McpController(toolRegistry, bridge);
    }

    /**
     * MCP 远程工具桥接调度器默认实现。
     *
     * <p>监听注册表变更，自动将启用连接的远程工具注册到 AiToolRegistry。
     * 同时维护桥接清单，支撑 MCP Server 分类暴露。</p>
     *
     * @param toolRegistry AI 工具注册表
     * @return McpRemoteToolBridge 实例
     */
    @Bean
    @ConditionalOnMissingBean(McpRemoteToolBridge.class)
    public McpRemoteToolBridge mcpRemoteToolBridge(AiToolRegistry toolRegistry) {
        log.info("已装配 DefaultMcpRemoteToolBridge（MCP 远程工具桥接调度器）");
        return new DefaultMcpRemoteToolBridge(toolRegistry);
    }

    /**
     * MCP 客户端注册表默认实现。
     *
     * <p>管理多个外部 MCP 服务连接的生命周期，启动时从配置中心加载已保存的连接列表。
     * 注入 McpRemoteToolBridge，在连接注册/注销时自动触发桥接/解桥。</p>
     *
     * @param configFacade 配置管理门面
     * @param bridge       MCP 远程工具桥接调度器
     * @return McpClientRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(McpClientRegistry.class)
    public McpClientRegistry mcpClientRegistry(ConfigFacade configFacade,
                                                McpRemoteToolBridge bridge) {
        log.info("已装配 DefaultMcpClientRegistry（MCP 客户端注册表，集成桥接）");
        return new DefaultMcpClientRegistry(configFacade, bridge);
    }

    /**
     * MCP 客户端业务服务默认实现。
     *
     * @param registry MCP 客户端注册表
     * @return McpClientService 实例
     */
    @Bean
    @ConditionalOnMissingBean(McpClientService.class)
    public McpClientService mcpClientService(McpClientRegistry registry,
                                              McpRemoteToolBridge bridge) {
        log.info("已装配 DefaultMcpClientService（MCP 客户端业务服务）");
        return new DefaultMcpClientService(registry, bridge);
    }

    /**
     * MCP 客户端连接默认实现（HTTP JSON-RPC）。
     *
     * <p>提供 HTTP 协议的 MCP 客户端连接能力，可通过声明同名 Bean 覆盖以支持 stdio/SSE 等协议。</p>
     *
     * @return McpClientConnection 实例
     */
    @Bean
    @ConditionalOnMissingBean(McpClientConnection.class)
    public McpClientConnection mcpClientConnection() {
        log.info("已装配 HttpMcpClientConnection（默认 HTTP JSON-RPC 实现）");
        return new HttpMcpClientConnection();
    }

    /**
     * 平台级 LangChain4j AIToolProvider 桥接。
     *
     * <p>将 AiToolRegistry 中所有工具（本地 + 桥接）无缝暴露给 LangChain4j 生态。
     * 仅在类路径中存在 LangChain4j AiServices 时装配，不影响无 LangChain4j 的项目。</p>
     *
     * @param toolRegistry AI 工具注册表
     * @return AIToolProvider 实例
     */
    @Bean
    @ConditionalOnClass(name = "dev.langchain4j.service.AiServices")
    @ConditionalOnMissingBean(AIToolProvider.class)
    public AIToolProvider aiToolProvider(
            AiToolRegistry toolRegistry) {
        log.info("已装配 AIToolProvider（LangChain4j 工具桥接）");
        return new AIToolProvider(toolRegistry);
    }
}
