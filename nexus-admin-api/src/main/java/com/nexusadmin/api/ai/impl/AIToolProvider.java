package com.nexusadmin.api.ai.impl;

import com.alibaba.fastjson2.JSON;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.ai.AiTool;
import com.nexusadmin.api.ai.AiToolRegistry;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台级 LangChain4j ToolProvider 桥接。
 *
 * <p>将 AiToolRegistry 中的所有工具（本地 + 桥接）无缝暴露给 LangChain4j 生态。
 * 开发者直接注入此 Bean，即可在 AiServices 中使用全部平台工具，
 * 无需手动将 AiTool 映射为 ToolSpecification。</p>
 *
 * <p><strong>使用示例：</strong>
 * <pre>{@code
 * @Autowired ChatLanguageModel model;
 * @Autowired AIToolProvider toolProvider;
 *
 * Assistant ai = AiServices.builder(Assistant.class)
 *         .chatLanguageModel(model)
 *         .toolProvider(toolProvider)
 *         .build();
 *
 * String result = ai.chat("审查 Service.java");
 * }</pre>
 *
 * <p>工具发现以 AiToolRegistry 为唯一权威来源——任何注册到 Registry 的工具
 * （无论来自 Spring Bean 扫描、插件运行时注册、还是 MCP 远程桥接）
 * 自动对 LangChain4j 可见。</p>
 */
public class AIToolProvider implements ToolProvider {

    private static final Logger log = LoggerFactory.getLogger(AIToolProvider.class);

    private final AiToolRegistry toolRegistry;

    public AIToolProvider(AiToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 向 LangChain4j 提供所有可用工具的映射。
     *
     * <p>LangChain4j 每次调用需要工具列表时（如 Agent 循环）都会回调此方法，
     * 因此始终能从 AiToolRegistry 获取最新工具列表（包括运行时动态注册/注销）。</p>
     *
     * @param request 包含 chatMemoryId 和 userMessage 的提供请求
     * @return ToolSpecification → ToolExecutor 的完整映射
     */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        for (AiTool tool : toolRegistry.listAll()) {
            ToolSpecification spec = ToolSpecification.builder()
                    .name(tool.getName())
                    .description(tool.getDescription())
                    .parameters(buildSchema(tool.getInputTypeSchema()))
                    .build();

            ToolExecutor executor = (toolRequest, memoryId) -> {
                Map<String, Object> args;
                try {
                    args = JSON.parseObject(toolRequest.arguments(), Map.class);
                } catch (Exception e) {
                    args = Map.of();
                }
                if (args == null) {
                    args = Map.of();
                }

                try {
                    AiTool.ToolResult result = tool.execute(args, InvocationContext.builder()
                            .channelId("AI_ASSISTANT")
                            .build());
                    return result.message();
                } catch (Exception e) {
                    log.error("执行工具失败: toolName={}", toolRequest.name(), e);
                    return "错误: " + e.getMessage();
                }
            };

            tools.put(spec, executor);
        }

        return new ToolProviderResult(tools);
    }

    /**
     * 获取所有工具规格，用于需要直接获取 ToolSpecification 列表的场景。
     *
     * <p>直接映射 AiToolRegistry.listAll() —— 本地工具和桥接工具统一处理。</p>
     *
     * @return ToolSpecification 列表
     */
    public List<ToolSpecification> getToolSpecifications() {
        return toolRegistry.listAll().stream()
                .map(tool -> ToolSpecification.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .parameters(buildSchema(tool.getInputTypeSchema()))
                        .build())
                .toList();
    }

    /**
     * 构建 LangChain4j JsonObjectSchema。
     *
     * <p>由于 AiTool 提供的 Schema 为 JSON 字符串，而 LangChain4j 1.0.0-beta1
     * 的 JsonObjectSchema 仅支持 Builder 模式构建。此处返回一个简单的空 Schema，
     * 实际参数的校验由工具实现层负责。</p>
     *
     * @param schema 工具参数 JSON Schema 字符串（暂未深度解析）
     * @return JsonObjectSchema
     */
    private JsonObjectSchema buildSchema(String schema) {
        // LangChain4j 1.0.0-beta1 的 JsonObjectSchema 不支持 JSON 字符串反序列化，
        // 仅支持通过 Builder 手动构建。由于 AiTool 的 Schema 格式灵活，
        // 此处返回一个空 Schema，参数的校验由 AiTool.execute() 内部完成。
        return JsonObjectSchema.builder().build();
    }
}
