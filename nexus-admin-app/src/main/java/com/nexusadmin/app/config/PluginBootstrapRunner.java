package com.nexusadmin.app.config;

import com.nexusadmin.app.config.properties.PlatformProperties;
import com.nexusadmin.plugin.loader.LoadedPlugin;
import com.nexusadmin.plugin.loader.PluginCandidate;
import com.nexusadmin.plugin.loader.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * 插件启动 Runner，在应用启动完成后扫描插件目录并加载（可选自动启动）所有插件。
 */
@Component
public class PluginBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PluginBootstrapRunner.class);

    /**
     * 插件管理器。
     */
    private final PluginManager pluginManager;
    /**
     * 平台配置属性。
     */
    private final PlatformProperties platformProperties;

    /**
     * 构造插件启动 Runner。
     *
     * @param pluginManager       插件管理器
     * @param platformProperties  平台配置属性
     */
    public PluginBootstrapRunner(PluginManager pluginManager,
                                 PlatformProperties platformProperties) {
        this.pluginManager = pluginManager;
        this.platformProperties = platformProperties;
    }

    /**
     * ApplicationRunner 回调，在应用启动后扫描插件目录并逐个加载。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("-----------------------------------------------");
        log.info("开始加载插件系统...");
        
        // 1. Discover 阶段
        log.info("开始插件发现阶段...");
        List<PluginCandidate> candidates = new java.util.ArrayList<>(pluginManager.discoverAll());
        
        // 扫描外部目录并加入候选列表
        String pluginPath = platformProperties.getPlugin().getPath();
        Path root = Paths.get(pluginPath).toAbsolutePath();
        if (Files.exists(root)) {
            try (Stream<Path> stream = Files.list(root)) {
                stream.filter(path -> Files.isDirectory(path) || path.toString().endsWith(".jar"))
                        .forEach(path -> {
                            try {
                                candidates.add(pluginManager.discover(path));
                            } catch (Exception e) {
                                log.warn("发现外部插件失败: {}", path, e);
                            }
                        });
            } catch (IOException ex) {
                log.warn("扫描插件目录失败：{}", root, ex);
            }
        }
        log.info("发现候选插件数: {}", candidates.size());
        
        // 2. Resolve 阶段
        log.info("开始插件解析去重...");
        List<PluginCandidate> resolved = pluginManager.resolve(candidates);
        
        // 3. Install 阶段
        log.info("开始插件安装...");
        pluginManager.installResolved(resolved);
        
        // 4. 启动阶段
        boolean autoStart = platformProperties.getPlugin().isAutoStart();
        int startedCount = 0;
        
        // 启动所有已安装的插件
        for (LoadedPlugin loaded : pluginManager.list()) {
            if (autoStart && loaded.descriptor().hasEntryPoint() && loaded.state() == com.nexusadmin.plugin.lifecycle.PluginState.INSTALLED) {
                try {
                    pluginManager.start(loaded.descriptor().id());
                    startedCount++;
                    log.info("插件已启动：{}", loaded.descriptor().id());
                } catch (Exception ex) {
                    log.warn("插件启动失败：{}", loaded.descriptor().id(), ex);
                }
            }
        }
        
        log.info("插件系统加载完成：当前共安装 {} 个插件，已启动 {} 个插件", pluginManager.list().size(), startedCount);
        log.info("-----------------------------------------------");
    }

}
