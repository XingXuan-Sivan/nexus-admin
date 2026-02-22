package com.nexusadmin.core.plugin.descriptor.reader;

import com.nexusadmin.core.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.core.exception.DescriptorParseException;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorParser;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorReader;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorFinder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.nexusadmin.core.plugin.descriptor.PluginDescriptorKeys.DESCRIPTOR_PATH;

/**
 * JSON 格式插件描述文件读取器。
 * <p>支持从目录（开发环境或打包环境）和 JAR 文件中读取 plugin.json。</p>
 */
public class JsonDescriptorReader implements PluginDescriptorReader {

    private final PluginDescriptorParser<InputStream> parser;
    private final List<PluginDescriptorFinder> finders;

    /**
     * 构造函数。
     *
     * @param parser  描述文件解析器
     * @param finders 描述文件查找器列表
     */
    public JsonDescriptorReader(PluginDescriptorParser<InputStream> parser,
                                List<PluginDescriptorFinder> finders) {
        this.parser = parser;
        this.finders = List.copyOf(finders != null ? finders : List.of());
    }

    @Override
    public PluginDescriptor read(Path pluginPath) {
        if (Files.isDirectory(pluginPath)) {
            return readFromDirectory(pluginPath);
        }
        return readFromJar(pluginPath);
    }

    @Override
    public boolean supports(Path pluginPath) {
        // 总是支持，作为默认读取器
        return true;
    }

    private PluginDescriptor readFromDirectory(Path dir) {
        // 按优先级排序查找器
        Optional<Path> descriptorPath = finders.stream()
                .sorted((f1, f2) -> Integer.compare(f2.priority(), f1.priority()))
                .map(f -> f.find(dir))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (descriptorPath.isEmpty()) {
            throw new DescriptorParseException("在目录中未找到插件描述文件: " + dir);
        }

        Path path = descriptorPath.get();
        try (InputStream is = Files.newInputStream(path)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new DescriptorParseException("读取描述文件失败: " + path, e);
        }
    }

    private PluginDescriptor readFromJar(Path jarPath) {
        if (!jarPath.toString().endsWith(".jar")) {
            throw new DescriptorParseException("不支持的文件类型: " + jarPath);
        }
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(DESCRIPTOR_PATH);
            if (entry == null) {
                throw new DescriptorParseException("JAR 中未找到 " + DESCRIPTOR_PATH + ": " + jarPath);
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                return parser.parse(is);
            }
        } catch (IOException e) {
            throw new DescriptorParseException("读取 JAR 描述文件失败: " + jarPath, e);
        }
    }
}
