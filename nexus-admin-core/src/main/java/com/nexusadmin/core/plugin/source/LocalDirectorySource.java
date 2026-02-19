package com.nexusadmin.core.plugin.source;

import com.nexusadmin.core.plugin.PluginDescriptor;
import com.nexusadmin.core.exception.PluginSourceException;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorReader;
import com.nexusadmin.core.plugin.loader.CandidatePlugin;
import com.nexusadmin.core.plugin.loader.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地目录插件源，扫描指定目录下的 JAR 文件和子目录。
 */
public class LocalDirectorySource implements PluginSource {

    private static final Logger log = LoggerFactory.getLogger(LocalDirectorySource.class);

    private final Path rootPath;
    private final PluginDescriptorReader descriptorReader;

    public LocalDirectorySource(Path rootPath, PluginDescriptorReader descriptorReader) {
        this.rootPath = rootPath.toAbsolutePath().normalize();
        this.descriptorReader = descriptorReader;
    }

    @Override
    public String sourceType() {
        return "local-directory";
    }

    @Override
    public List<CandidatePlugin> scan() {
        List<CandidatePlugin> candidates = new ArrayList<>();

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

    private void scanPath(Path path, List<CandidatePlugin> candidates) {
        try {
            PluginDescriptor descriptor = descriptorReader.read(path);
            CandidatePlugin candidate = new CandidatePlugin(
                    descriptor.id(),
                    descriptor,
                    path,
                    null,
                    SourceType.EXTERNAL
            );
            candidates.add(candidate);
            log.debug("发现外部插件: {} (路径: {})", descriptor.id(), path);
        } catch (Exception e) {
            log.warn("从路径发现插件失败: {}", path, e);
        }
    }

    private boolean isValidPluginPath(Path path) {
        return Files.isDirectory(path) ||
                (Files.isRegularFile(path) && path.toString().endsWith(".jar"));
    }
}
