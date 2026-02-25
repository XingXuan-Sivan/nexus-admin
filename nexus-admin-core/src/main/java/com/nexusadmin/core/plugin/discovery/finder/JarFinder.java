package com.nexusadmin.core.plugin.discovery.finder;

import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;

import java.nio.file.Path;
import java.util.Optional;

import static com.nexusadmin.core.plugin.discovery.PluginDescriptorKeys.DESCRIPTOR_PATH;

/**
 * JAR 文件查找器。
 * <p>用于查找 JAR 文件类型的描述文件路径。</p>
 */
public class JarFinder implements PluginDescriptorFinder {

    @Override
    public Optional<Path> find(Path pluginPath) {
        if (!pluginPath.toString().endsWith(".jar")) {
            return Optional.empty();
        }
        // JAR 文件使用内部路径，返回一个标记路径
        return Optional.of(pluginPath.resolve(DESCRIPTOR_PATH));
    }

    @Override
    public int priority() {
        return 30;
    }
}
