package com.nexusadmin.core.plugin.discovery.impl;

import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.nexusadmin.core.plugin.discovery.PluginDescriptorKeys.DESCRIPTOR_DEV_PATH;

/**
 * 开发环境目录查找器。
 * <p>用于查找 Maven 源码目录中的描述文件路径。</p>
 */
public class DevDirectoryFinder implements PluginDescriptorFinder {

    @Override
    public Optional<Path> find(Path pluginPath) {
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
