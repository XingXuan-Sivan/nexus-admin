package com.nexusadmin.api.service.impl;

import com.nexusadmin.api.service.SystemHealthProvider;
import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.PluginState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 健康检查的默认实现，聚合插件状态与 JVM 信息。
 */
public class DefaultSystemHealthProvider implements SystemHealthProvider {

    private final PluginFacade pluginFacade;

    public DefaultSystemHealthProvider(PluginFacade pluginFacade) {
        this.pluginFacade = pluginFacade;
    }

    @Override
    public HealthStatus getOverallHealth() {
        long failed = pluginFacade.listByState(PluginState.FAILED).size();
        if (failed > 0) {
            return HealthStatus.DEGRADED;
        }
        return HealthStatus.UP;
    }

    @Override
    public List<ComponentHealth> getComponentHealths() {
        List<ComponentHealth> healths = new ArrayList<>();

        int totalPlugins = pluginFacade.listPlugins().size();
        int activePlugins = pluginFacade.listByState(PluginState.ACTIVE).size();
        int failedPlugins = pluginFacade.listByState(PluginState.FAILED).size();

        healths.add(new ComponentHealth("插件系统",
                failedPlugins == 0 ? HealthStatus.UP : HealthStatus.DEGRADED,
                "共 " + totalPlugins + " 个插件，活跃 " + activePlugins + " 个",
                Map.of("total", totalPlugins, "active", activePlugins, "failed", failedPlugins)));

        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        healths.add(new ComponentHealth("JVM 内存",
                HealthStatus.UP,
                "已用 " + ((totalMemory - freeMemory) / (1024 * 1024)) + "MB / 最大 " + (runtime.maxMemory() / (1024 * 1024)) + "MB",
                Map.of("freeMemory", freeMemory, "totalMemory", totalMemory, "maxMemory", runtime.maxMemory())));

        healths.add(new ComponentHealth("处理器",
                HealthStatus.UP,
                "可用处理器 " + runtime.availableProcessors() + " 个",
                Map.of("availableProcessors", runtime.availableProcessors())));

        return healths;
    }
}
