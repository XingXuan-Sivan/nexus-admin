package com.nexusadmin.api.facade.impl;

import com.nexusadmin.api.facade.PlatformInfoView;
import com.nexusadmin.api.facade.SystemStatusFacade;
import com.nexusadmin.api.facade.SystemStatusView;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.config.ConfigManager;

import java.util.Map;

/**
 * 系统状态门面实现类。
 */
public class SystemStatusFacadeImpl implements SystemStatusFacade {

    private final PluginManager pluginManager;
    private final ConfigManager configManager;

    /**
     * 构造系统状态门面。
     *
     * @param pluginManager 插件管理器
     * @param configManager 配置管理器
     */
    public SystemStatusFacadeImpl(PluginManager pluginManager, ConfigManager configManager) {
        this.pluginManager = pluginManager;
        this.configManager = configManager;
    }

    @Override
    public SystemStatusView getStatus() {
        int totalPlugins = pluginManager.list().size();
        int activePlugins = (int) pluginManager.list().stream()
                .filter(w -> w.state() == PluginState.ACTIVE)
                .count();
        int disabledPlugins = (int) pluginManager.list().stream()
                .filter(w -> w.state() == PluginState.DISABLED)
                .count();
        int failedPlugins = (int) pluginManager.list().stream()
                .filter(w -> w.state() == PluginState.FAILED)
                .count();

        Map<String, String> jvmInfo = Map.of(
                "totalMemory", String.valueOf(Runtime.getRuntime().totalMemory()),
                "freeMemory", String.valueOf(Runtime.getRuntime().freeMemory()),
                "availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors())
        );

        return new SystemStatusView(
                SystemStatusView.STATUS_UP,
                totalPlugins,
                activePlugins,
                disabledPlugins,
                failedPlugins,
                0L,
                jvmInfo,
                Map.of()
        );
    }

    @Override
    public PlatformInfoView getPlatformInfo() {
        String name = configManager.get("platform", "infoName").orElse("Nexus Admin");
        String version = configManager.get("platform", "infoVersion").orElse("0.1.0-SNAPSHOT");
        String description = configManager.get("platform", "infoDescription").orElse("插件化系统拓展平台");
        return new PlatformInfoView(
                name,
                version,
                description,
                Map.of("javaVersion", System.getProperty("java.version")),
                Map.of("osName", System.getProperty("os.name"))
        );
    }

    @Override
    public boolean isHealthy() {
        return pluginManager.list().stream()
                .filter(w -> w.state() == PluginState.FAILED)
                .count() == 0;
    }
}
