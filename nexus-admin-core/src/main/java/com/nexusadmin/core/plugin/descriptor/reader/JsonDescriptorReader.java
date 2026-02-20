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
    private final PluginDescriptorFinder finder;

    /**
     * 构造函数。
     *
     * @param parser  描述文件解析器
     * @param finder  描述文件查找器（通常是组合查找器）
     */
    public JsonDescriptorReader(PluginDescriptorParser<InputStream> parser,
                                PluginDescriptorFinder finder) {
        this.parser = parser;
        this.finder = finder;
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
        Optional<Path> descriptorPath = finder.find(dir);
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
