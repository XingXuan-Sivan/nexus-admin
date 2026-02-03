package com.nexusadmin.core.spi.ai;

import com.nexusadmin.core.context.CoreContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行器 SPI，用于根据工具调用请求执行外部工具并返回结果。
 */
public interface ToolExecutor {
    /**
     * 执行工具调用请求。
     *
     * @param call    工具调用请求
     * @param context 平台上下文
     * @return 工具执行结果
     */
    ToolResult execute(ToolCall call, CoreContext context);

    /**
     * 工具调用请求记录
     *
     * @param toolName  工具名称
     * @param requestId 请求 ID
     * @param arguments 调用参数
     */
    record ToolCall(String toolName,
                    String requestId,
                    Map<String, Object> arguments) {
        public ToolCall {
            arguments = arguments == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(arguments));
        }
    }

    /**
     * 工具执行结果记录
     *
     * @param toolName  工具名称
     * @param requestId 请求 ID
     * @param success   是否执行成功
     * @param output    输出结果
     * @param metadata  元数据
     */
    record ToolResult(String toolName,
                      String requestId,
                      boolean success,
                      String output,
                      Map<String, String> metadata) {
        public ToolResult {
            metadata = metadata == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }
}
