package com.nexusadmin.api.spi.routing;

import com.nexusadmin.api.context.CoreContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 路由策略 SPI，用于根据请求路径、方法等信息决定目标路由或是否放行。
 */
public interface RoutingPolicy {
    /**
     * 根据路由请求决定目标路由。
     *
     * @param request 路由请求
     * @param context 平台上下文
     * @return 路由决策结果
     */
    RouteDecision decide(RouteRequest request, CoreContext context);

    /**
     * 路由请求。
     *
     * @param routeId   路由 ID
     * @param path      请求路径
     * @param method    请求方法
     * @param attributes  请求属性
     */
    record RouteRequest(String routeId,
                        String path,
                        String method,
                        Map<String, String> attributes) {
        public RouteRequest {
            attributes = attributes == null ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(attributes));
        }
    }

    /**
     * 路由决策结果。
     *
     * @param allowed 是否允许访问
     * @param target  目标路由 ID
     * @param reason  决策原因
     */
    record RouteDecision(boolean allowed,
                         String target,
                         String reason) {
    }
}
