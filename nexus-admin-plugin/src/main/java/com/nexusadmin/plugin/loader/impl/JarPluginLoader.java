package com.nexusadmin.plugin.loader.impl;

import com.nexusadmin.core.spi.SpiRegistry;
import com.nexusadmin.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.plugin.descriptor.PluginDescriptorParser;
import com.nexusadmin.plugin.descriptor.parser.YamlPluginDescriptorParser;
import com.nexusadmin.plugin.exception.PluginLoadException;
import com.nexusadmin.plugin.lifecycle.Plugin;
import com.nexusadmin.plugin.loader.LoadedPlugin;
import com.nexusadmin.plugin.loader.PluginCandidate;
import com.nexusadmin.plugin.loader.PluginLoader;
import com.nexusadmin.plugin.loader.SourceType;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * JAR 插件加载器，负责从指定路径（JAR 文件或目录）加载插件并构建 {@link LoadedPlugin} 对象。
 * <p>针对开发环境进行了优化：如果给定路径是一个目录且包含 Maven 编译输出目录（target/classes），则自动使用该目录作为类路径。</p>
 */
public class JarPluginLoader implements PluginLoader {
    private final PluginDescriptorParser<Map<String, Object>> descriptorParser = new YamlPluginDescriptorParser();

    @Override
    public boolean canLoad(Path path) {
        if (path == null) return false;
        // 支持 JAR 文件或目录（开发环境）
        return Files.isRegularFile(path) && path.toString().endsWith(".jar")
                || Files.isDirectory(path);
    }

    @Override
    public PluginCandidate discover(Path path, SpiRegistry registry) {
        if (path == null) {
            throw new PluginLoadException("plugin path is null");
        }
        PluginDescriptor descriptor = descriptorParser.load(path);
        return new PluginCandidate(descriptor.id(), descriptor, path, this, SourceType.EXTERNAL);
    }

    @Override
    public LoadedPlugin load(PluginCandidate candidate, SpiRegistry registry) {
        Path path = candidate.sourcePath();
        if (path == null) {
            throw new PluginLoadException("plugin path is null");
        }

        // 开发环境优化：如果是 Maven 模块目录且不直接包含包根目录，自动指向 target/classes
        Path classPath = path;
        if (Files.isDirectory(path) && !Files.exists(path.resolve("com"))) {
            Path targetClasses = path.resolve("target").resolve("classes");
            if (Files.exists(targetClasses)) {
                classPath = targetClasses;
            }
        }

        PluginDescriptor descriptor = candidate.descriptor();
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{classPath.toUri().toURL()},
                    Thread.currentThread().getContextClassLoader()
            );
            Plugin plugin = null;
            if (descriptor.hasEntryPoint()) {
                Class<?> pluginClass = classLoader.loadClass(descriptor.mainClass());
                if (!Plugin.class.isAssignableFrom(pluginClass)) {
                    throw new PluginLoadException("mainClass does not implement Plugin: " + descriptor.mainClass());
                }
                plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
            }
            return new LoadedPlugin(descriptor, plugin, classLoader, path);
        } catch (Exception ex) {
            throw new PluginLoadException("failed to load plugin: " + path, ex);
        }
    }

    /**
     * JAR 插件加载器支持物理卸载。
     *
     * @return 始终返回 true
     */
    @Override
    public boolean supportsRemove() {
        return true;
    }

    /**
     * 删除指定的插件文件或目录。
     *
     * @param plugin 已加载插件
     */
    @Override
    public void remove(LoadedPlugin plugin) {
        Path path = plugin.pluginPath();
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                // 递归删除目录
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                // 删除单文件
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new PluginLoadException("物理卸载插件失败，无法删除路径: " + path, e);
        }
    }
}
