package com.nexusadmin.api.spi.ai;

import com.nexusadmin.api.context.CoreContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 对话模型 SPI，负责根据会话上下文和用户消息生成回复。
 */
public interface ChatProvider {
    /**
     * 执行一次对话请求，根据输入消息和上下文返回回复。
     *
     * @param request 对话请求
     * @param context 平台上下文
     * @return 对话响应
     */
    ChatResponse chat(ChatRequest request, CoreContext context);

    /**
     * 构造对话请求参数。
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @param message   用户消息
     * @param parameters 其他参数
     */
    record ChatRequest(String sessionId,
                       String userId,
                       String message,
                       Map<String, String> parameters) {
        public ChatRequest {
            parameters = parameters == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(parameters));
        }
    }

    /**
     * 构造对话响应结果。
     *
     * @param sessionId 会话 ID
     * @param reply     回复内容
     * @param metadata  元数据
     */
    record ChatResponse(String sessionId,
                        String reply,
                        Map<String, String> metadata) {
        public ChatResponse {
            metadata = metadata == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }
}
