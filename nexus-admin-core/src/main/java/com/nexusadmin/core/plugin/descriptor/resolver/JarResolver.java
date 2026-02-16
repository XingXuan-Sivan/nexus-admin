package com.nexusadmin.core.plugin.descriptor.resolver;

import com.nexusadmin.core.plugin.descriptor.PluginDescriptorPathResolver;

import java.nio.file.Path;
import java.util.Optional;

import static com.nexusadmin.core.plugin.descriptor.PluginDescriptorKeys.DESCRIPTOR_PATH;

/**
 * JAR 文件解析器。
 * <p>用于标记 JAR 文件类型的描述文件路径。</p>
 */
public class JarResolver implements PluginDescriptorPathResolver {

    @Override
    public Optional<Path> resolve(Path pluginPath) {
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
