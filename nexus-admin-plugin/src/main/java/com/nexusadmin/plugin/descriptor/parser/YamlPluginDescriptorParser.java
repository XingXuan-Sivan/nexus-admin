package com.nexusadmin.plugin.descriptor.parser;

import com.nexusadmin.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.plugin.descriptor.PluginDescriptorParser;
import com.nexusadmin.plugin.descriptor.impl.DefaultPluginDescriptor;
import com.nexusadmin.plugin.exception.PluginDescriptorException;
import com.nexusadmin.plugin.exception.PluginLoadException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.nexusadmin.plugin.constant.PluginDescriptorConstants.*;

/**
 * 基于 YAML 解析出的 Map 结构的插件描述解析器。
 */
public class YamlPluginDescriptorParser implements PluginDescriptorParser<Map<String, Object>> {

    private final Yaml yaml = new Yaml();

    @Override
    public PluginDescriptor parse(Map<String, Object> source) {
        if (source == null) {
            throw new PluginDescriptorException(PLUGIN_DESCRIPTOR_FILE + " 为空");
        }

        Object idValue = source.get(KEY_ID);
        Object versionValue = source.get(KEY_VERSION);
        Object mainClassValue = source.get(KEY_MAIN_CLASS);
        Object providesValue = source.get(KEY_PROVIDES);
        Object priorityValue = source.get(KEY_PRIORITY);

        List<String> provides = new ArrayList<>();
        if (providesValue instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    provides.add(Objects.toString(item));
                }
            }
        } else if (providesValue instanceof String text && !text.isBlank()) {
            provides.add(text);
        }

        Map<String, Object> metadata = new HashMap<>(source);
        metadata.remove(KEY_ID);
        metadata.remove(KEY_VERSION);
        metadata.remove(KEY_MAIN_CLASS);
        metadata.remove(KEY_PROVIDES);
        metadata.remove(KEY_PRIORITY);

        int priority = 0;
        if (priorityValue instanceof Number num) {
            priority = num.intValue();
        } else if (priorityValue instanceof String text) {
            try {
                priority = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {}
        }

        return new DefaultPluginDescriptor(
                idValue == null ? "" : Objects.toString(idValue),
                versionValue == null ? "" : Objects.toString(versionValue),
                mainClassValue == null ? "" : Objects.toString(mainClassValue),
                provides,
                metadata,
                priority
        );
    }

    @Override
    public PluginDescriptor load(Path path) {
        if (path == null) {
            throw new PluginDescriptorException("插件路径不能为空");
        }
        if (Files.isDirectory(path)) {
            return loadFromDirectory(path);
        }
        if (path.toString().endsWith(".jar")) {
            return loadFromJar(path);
        }
        throw new PluginDescriptorException("不支持的插件路径: " + path);
    }

    /**
     * 从目录中加载插件描述。
     *
     * @param directory 插件目录
     * @return 插件描述
     */
    private PluginDescriptor loadFromDirectory(Path directory) {
        Path descriptorPath = directory.resolve(PLUGIN_DESCRIPTOR_FILE);
        if (!Files.exists(descriptorPath)) {
            throw new PluginDescriptorException("在目录 " + directory + " 中未找到 " + PLUGIN_DESCRIPTOR_FILE + " 文件");
        }
        try (InputStream input = Files.newInputStream(descriptorPath)) {
            Map<String, Object> raw = yaml.load(input);
            return parse(raw);
        } catch (IOException ex) {
            throw new PluginLoadException("读取 " + PLUGIN_DESCRIPTOR_FILE + " 文件失败: " + descriptorPath, ex);
        }
    }

    /**
     * 从 JAR 文件中加载插件描述。
     *
     * @param jarPath JAR 文件路径
     * @return 插件描述
     */
    private PluginDescriptor loadFromJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(PLUGIN_DESCRIPTOR_FILE);
            if (entry == null) {
                throw new PluginDescriptorException("在 JAR 包 " + jarPath + " 中未找到 " + PLUGIN_DESCRIPTOR_FILE + " 文件");
            }
            try (InputStream input = jarFile.getInputStream(entry)) {
                Map<String, Object> raw = yaml.load(input);
                return parse(raw);
            }
        } catch (IOException ex) {
            throw new PluginLoadException("从 JAR 包读取 " + PLUGIN_DESCRIPTOR_FILE + " 文件失败: " + jarPath, ex);
        }
    }
}
