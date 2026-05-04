package com.nexusadmin.api.domain.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统状态视图，用于展示系统运行状态。
 *
 * @param status           系统状态（UP、DOWN、DEGRADED）
 * @param totalPlugins     插件总数
 * @param activePlugins    活跃插件数
 * @param disabledPlugins  禁用插件数
 * @param failedPlugins    失败插件数
 * @param uptimeMillis     运行时长（毫秒）
 * @param jvmInfo          JVM 信息
 * @param attributes       扩展属性
 */
public record SystemStatusView(String status,
                               int totalPlugins,
                               int activePlugins,
                               int disabledPlugins,
                               int failedPlugins,
                               long uptimeMillis,
                               Map<String, String> jvmInfo,
                               Map<String, String> attributes) {

    /**
     * 系统状态常量。
     */
    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";
    public static final String STATUS_DEGRADED = "DEGRADED";

    /**
     * 创建系统状态视图。
     */
    public SystemStatusView {
        status = status != null ? status : STATUS_UP;
        jvmInfo = jvmInfo == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(jvmInfo));
        attributes = attributes == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
