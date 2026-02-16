package com.nexusadmin.core.plugin.descriptor.resolver;

import com.nexusadmin.core.plugin.descriptor.PluginDescriptorPathResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.nexusadmin.core.plugin.descriptor.PluginDescriptorKeys.DESCRIPTOR_DEV_PATH;

/**
 * 开发环境目录解析器。
 * <p>用于解析 Maven 源码目录中的描述文件路径。</p>
 */
public class DevDirectoryResolver implements PluginDescriptorPathResolver {

    @Override
    public Optional<Path> resolve(Path pluginPath) {
        if (!Files.isDirectory(pluginPath)) {
            return Optional.empty();
        }
        Path descriptorPath = pluginPath.resolve(DESCRIPTOR_DEV_PATH);
        return Files.exists(descriptorPath) ? Optional.of(descriptorPath) : Optional.empty();
    }

    @Override
    public int priority() {
        return 20;
    }
}
