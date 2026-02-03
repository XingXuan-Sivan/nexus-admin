package com.nexusadmin.core.spi.ai;

import com.nexusadmin.core.context.CoreContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能体策略 SPI，用于根据请求与平台上下文决定是否允许某个 Agent 以及下发相关参数。
 */
public interface AgentPolicy {
    /**
     * 根据请求决定是否允许 Agent 运行。
     *
     * @param request Agent 决策请求
     * @param context 平台上下文
     * @return Agent 决策结果
     */
    AgentDecision decide(AgentRequest request, CoreContext context);

    /**
     * Agent 决策结果记录
     *
     * @param allowed 是否允许执行
     * @param reason 决策原因
     * @param parameters 决策参数
     */
    record AgentRequest(String agentId,
                        String goal,
                        Map<String, String> context) {
        public AgentRequest {
            context = context == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(context));
        }
    }

    /**
     * Agent 决策结果记录
     *
     * @param allowed 是否允许执行
     * @param reason 决策原因
     * @param parameters 决策参数
     */
    record AgentDecision(boolean allowed,
                         String reason,
                         Map<String, String> parameters) {
        public AgentDecision {
            parameters = parameters == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(parameters));
        }
    }
}
