package com.nexusadmin.api.extension.ai;

import com.nexusadmin.api.context.InvocationContext;

import java.util.Map;

/**
 * AI 工具接口，平台级工具抽象，兼容 MCP 协议与 LangChain4j @Tool 注解。
 *
 * <p>工具是平台暴露给 AI 的"函数"——AI 理解工具的描述和参数 Schema，
 * 在对话中决定调用哪个工具来完成用户意图。命名与 LangChain4j @Tool 统一，
 * 降低开发者理解成本。</p>
 *
 * <p><strong>实现者需覆写：</strong>
 * <ul>
 *   <li>{@link #getName()} — 工具唯一名称</li>
 *   <li>{@link #getDescription()} — 工具描述</li>
 *   <li>{@link #getInputTypeSchema()} — 参数 JSON Schema</li>
 *   <li>{@link #execute(Map, InvocationContext)} — 执行工具</li>
 * </ul>
 *
 * <p><strong>平台内建工具示例：</strong>
 * <ul>
 *   <li>plugin.list — 列出所有插件</li>
 *   <li>plugin.start — 启动指定插件</li>
 *   <li>plugin.stop — 停止指定插件</li>
 *   <li>system.status — 获取系统运行状态</li>
 *   <li>config.get — 读取配置</li>
 *   <li>config.update — 更新配置</li>
 *   <li>log.query — 查询日志</li>
 * </ul>
 */
public interface AiTool {

    /**
     * 获取工具唯一名称。
     *
     * @return 工具名称，如 "plugin.list"
     */
    String getName();

    /**
     * 获取工具描述，供 AI 理解工具用途。
     *
     * @return 工具描述文本
     */
    String getDescription();

    /**
     * 获取输入参数的 JSON Schema 字符串。
     *
     * @return 参数 JSON Schema，无参数时返回空字符串
     */
    String getInputTypeSchema();

    /**
     * 执行工具（携带 InvocationContext）。
     *
     * @param arguments 调用参数
     * @param context   调用上下文
     * @return 工具执行结果
     */
    ToolResult execute(Map<String, Object> arguments, InvocationContext context);

    /**
     * 以 JSON 字符串参数调用工具（桥接 MCP / LangChain4j 通用调用）。
     * 解析 JSON 参数，构建 InvocationContext，委托 execute()。
     *
     * @param jsonInput JSON 格式的调用参数
     * @return JSON 格式的执行结果
     */
    default String call(String jsonInput) {
        Map<String, Object> args;
        try {
            com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(jsonInput);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) (Map) obj;
            args = parsed != null ? parsed : Map.of();
        } catch (Exception e) {
            args = Map.of();
        }
        InvocationContext ctx = InvocationContext.builder()
                .channelId("AI_AGENT")
                .build();
        ToolResult result = execute(args, ctx);
        return com.alibaba.fastjson2.JSON.toJSONString(result);
    }

    /**
     * 工具执行结果。
     *
     * @param success 是否执行成功
     * @param message 结果消息
     * @param data    返回数据
     */
    record ToolResult(boolean success, String message, Object data) {
    }
}
