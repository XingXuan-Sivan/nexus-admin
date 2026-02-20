package com.nexusadmin.app.config;

import com.nexusadmin.app.config.properties.PlatformProperties;
import com.nexusadmin.core.plugin.PluginState;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWapper;
import com.nexusadmin.core.plugin.PluginManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台核心的启动 Runner，在应用启动完成后扫描插件目录并加载（可选自动启动）所有插件。
 */
@Component
@Order(Integer.MAX_VALUE - 1)
public class BootstrapRunner implements ApplicationRunner {

    private final PluginManager pluginManager;
    private final PlatformProperties platformProperties;

    public BootstrapRunner(PluginManager pluginManager,
                           PlatformProperties platformProperties) {
        this.pluginManager = pluginManager;
        this.platformProperties = platformProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 1. Discover 阶段
        List<PluginMetadata> candidates = pluginManager.discover();

        // 2. Resolve 阶段
        List<PluginMetadata> resolved = pluginManager.resolve(candidates);

        // 3. Install 阶段
        pluginManager.install(resolved);

        // 4. Auto Start
        if (platformProperties.getPlugin().isAutoStart()) {
            autoStartPlugins();
        }
    }

    private void autoStartPlugins() {
        for (PluginWapper loaded : pluginManager.list()) {
            if (loaded.descriptor().hasEntryPoint() && loaded.state() == PluginState.INSTALLED) {
                try {
                    pluginManager.start(loaded.descriptor().id());
                } catch (Exception ex) {
                    // 启动失败已在事件监听器中记录
                }
            }
        }
    }
}
