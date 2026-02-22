package com.nexusadmin.app.config;

import com.nexusadmin.core.plugin.PluginManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 平台核心的启动 Runner，在应用启动完成后执行插件系统启动流程。
 */
@Component
@Order(Integer.MAX_VALUE - 1)
public class BootstrapRunner implements ApplicationRunner {

    private final PluginManager pluginManager;

    public BootstrapRunner(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        pluginManager.bootstrap();
    }
}
