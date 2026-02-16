package com.nexusadmin.core.plugin.descriptor.resolver;

import com.nexusadmin.core.plugin.descriptor.PluginDescriptorPathResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.nexusadmin.core.plugin.descriptor.PluginDescriptorKeys.DESCRIPTOR_PATH;

/**
 * 打包目录解析器。
 * <p>用于解析已打包目录（如 target/classes）中的描述文件路径。</p>
 */
public class PackedDirectoryResolver implements PluginDescriptorPathResolver {

    @Override
    public Optional<Path> resolve(Path pluginPath) {
        if (!Files.isDirectory(pluginPath)) {
            return Optional.empty();
        }
        Path descriptorPath = pluginPath.resolve(DESCRIPTOR_PATH);
        return Files.exists(descriptorPath) ? Optional.of(descriptorPath) : Optional.empty();
    }

    @Override
    public int priority() {
        return 10;
    }
}
