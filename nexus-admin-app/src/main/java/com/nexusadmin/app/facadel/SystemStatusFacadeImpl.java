package com.nexusadmin.app.facadel;

import com.nexusadmin.api.management.PlatformInfoView;
import com.nexusadmin.api.management.SystemStatusFacade;
import com.nexusadmin.api.management.SystemStatusView;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;

import java.util.Map;

/**
 * 系统状态门面实现类。
 */
public class SystemStatusFacadeImpl implements SystemStatusFacade {

    private final PluginManager pluginManager;

    /**
     * 构造系统状态门面。
     *
     * @param pluginManager 插件管理器
     */
    public SystemStatusFacadeImpl(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
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
        return new PlatformInfoView(
                "Nexus Admin",
                "0.1.0-SNAPSHOT",
                "插件化业务拓展平台",
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
