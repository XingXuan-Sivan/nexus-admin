package com.nexusadmin.core.plugin.discovery.source;

import com.nexusadmin.core.plugin.discovery.PluginDescriptor;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.exception.PluginSourceException;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地目录插件源，扫描指定目录下的 JAR 文件和子目录。
 */
public class LocalDirectorySource implements PluginSource {

    private static final Logger log = LoggerFactory.getLogger(LocalDirectorySource.class);

    private final Path rootPath;
    private final List<PluginDescriptorParser> parsers;

    /**
     * 单个插件的源实现，持有具体插件的路径信息。
     */
    private static class PluginSourceImpl implements PluginSource {

        private final Path physicalPath;
        private final URL[] classpath;

        PluginSourceImpl(Path physicalPath, URL[] classpath) {
            this.physicalPath = physicalPath;
            this.classpath = classpath.clone();
        }

        @Override
        public SourceType getType() {
            return SourceType.EXTERNAL;
        }

        @Override
        public URL[] getClasspath() {
            return classpath.clone();
        }

        @Override
        public Path getPhysicalPath() {
            return physicalPath;
        }

        @Override
        public List<PluginMetadata> scan() {
            return List.of();
        }

        @Override
        public boolean supportsPhysicalRemoval() {
            return true;
        }

        @Override
        public void removePhysically() {
            if (physicalPath == null || !Files.exists(physicalPath)) {
                return;
            }
            try {
                if (Files.isDirectory(physicalPath)) {
                    Files.walkFileTree(physicalPath, new SimpleFileVisitor<>() {
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
                    Files.delete(physicalPath);
                }
            } catch (IOException e) {
                throw new PluginSourceException("物理卸载插件失败，无法删除路径: " + physicalPath, e);
            }
        }
    }

    public LocalDirectorySource(Path rootPath, List<PluginDescriptorParser> parsers) {
        this.rootPath = rootPath.toAbsolutePath().normalize();
        this.parsers = List.copyOf(parsers != null ? parsers : List.of());
    }

    @Override
    public SourceType getType() {
        return SourceType.EXTERNAL;
    }

    @Override
    public URL[] getClasspath() {
        return new URL[0];
    }

    @Override
    public Path getPhysicalPath() {
        return rootPath;
    }

    @Override
    public List<PluginMetadata> scan() {
        List<PluginMetadata> candidates = new ArrayList<>();

        if (!Files.exists(rootPath)) {
            log.debug("插件目录不存在，跳过扫描: {}", rootPath);
            return candidates;
        }

        if (!Files.isDirectory(rootPath)) {
            log.warn("插件路径不是目录: {}", rootPath);
            return candidates;
        }

        try (Stream<Path> stream = Files.list(rootPath)) {
            stream.filter(this::isValidPluginPath)
                    .forEach(path -> scanPath(path, candidates));
        } catch (IOException e) {
            throw new PluginSourceException("扫描插件目录失败: " + rootPath, e);
        }

        return candidates;
    }

    private void scanPath(Path path, List<PluginMetadata> candidates) {
        try {
            PluginDescriptorParser parser = parsers.stream()
                    .filter(p -> p.supports(path))
                    .findFirst()
                    .orElseThrow(() -> new PluginSourceException("无可用解析器: " + path));

            PluginDescriptor descriptor = parser.parse(path);

            // 为每个插件创建独立的 Source 实例
            URL[] classpath = resolveClasspath(path);
            PluginSourceImpl pluginSource = new PluginSourceImpl(path, classpath);

            PluginMetadata candidate = new PluginMetadata(descriptor.id(), descriptor, pluginSource);
            candidates.add(candidate);
            log.debug("发现外部插件: {} (路径: {})", descriptor.id(), path);
        } catch (Exception e) {
            log.warn("从路径发现插件失败: {}", path, e);
        }
    }

    /**
     * 解析插件路径对应的类路径URL数组。
     *
     * @param path 插件路径
     * @return 类路径URL数组
     */
    private URL[] resolveClasspath(Path path) {
        try {
            // 开发环境优化：如果是 Maven 模块目录且不直接包含包根目录，自动指向 target/classes
            Path classPath = path;
            if (Files.isDirectory(path) && !Files.exists(path.resolve("com"))) {
                Path targetClasses = path.resolve("target").resolve("classes");
                if (Files.exists(targetClasses)) {
                    classPath = targetClasses;
                }
            }
            return new URL[]{classPath.toUri().toURL()};
        } catch (MalformedURLException e) {
            throw new PluginSourceException("无法转换为URL: " + path, e);
        }
    }

    private boolean isValidPluginPath(Path path) {
        return Files.isDirectory(path) ||
                (Files.isRegularFile(path) && path.toString().endsWith(".jar"));
    }
}
