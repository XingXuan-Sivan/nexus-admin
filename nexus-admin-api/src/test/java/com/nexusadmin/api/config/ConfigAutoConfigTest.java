package com.nexusadmin.api.config;

import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.facade.ConfigFacade;
import com.nexusadmin.core.runtime.ConfigRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigAutoConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void bridgeBeans_shouldAlwaysComeFromTheSameRuntimeAggregate() {
        ConfigRuntime replacement = ConfigRuntime.builder()
                .configDir(tempDir)
                .eventBus(new SyncEventBus())
                .build();

        new ApplicationContextRunner()
                .withUserConfiguration(ConfigAutoConfig.class)
                .withBean(EventBus.class, SyncEventBus::new)
                .withBean(ConfigRuntime.class, () -> replacement)
                .run(context -> {
                    ConfigRuntime runtime = context.getBean(ConfigRuntime.class);
                    ConfigManager manager = context.getBean(ConfigManager.class);
                    ConfigFacade facade = context.getBean(ConfigFacade.class);

                    assertSame(replacement, runtime);
                    assertSame(runtime.configManager(), manager);
                    assertSame(manager, facade.configManager());
                });
    }
}
