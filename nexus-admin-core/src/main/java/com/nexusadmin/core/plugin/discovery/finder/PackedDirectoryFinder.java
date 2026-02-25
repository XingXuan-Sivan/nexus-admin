package com.nexusadmin.core.plugin.discovery.finder;

import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.nexusadmin.core.plugin.discovery.PluginDescriptorKeys.DESCRIPTOR_PATH;

/**
 * 打包目录查找器。
 * <p>用于查找已打包目录（如 target/classes）中的描述文件路径。</p>
 */
public class PackedDirectoryFinder implements PluginDescriptorFinder {

    @Override
    public Optional<Path> find(Path pluginPath) {
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
