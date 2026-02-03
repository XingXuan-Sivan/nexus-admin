package com.nexusadmin.plugin.loader.impl;

import com.nexusadmin.core.spi.SpiRegistry;
import com.nexusadmin.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.plugin.descriptor.PluginDescriptorParser;
import com.nexusadmin.plugin.descriptor.parser.YamlPluginDescriptorParser;
import com.nexusadmin.plugin.lifecycle.Plugin;
import com.nexusadmin.plugin.loader.LoadedPlugin;
import com.nexusadmin.plugin.loader.PluginCandidate;
import com.nexusadmin.plugin.loader.PluginLoader;
import com.nexusadmin.plugin.loader.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static com.nexusadmin.plugin.constant.PluginDescriptorConstants.PLUGIN_DESCRIPTOR_FILE;

/**
 * 类路径插件加载器，负责从当前运行环境的类路径中自动探测并加载插件。
 * <p>它会寻找类路径下所有包含 {@link com.nexusadmin.plugin.constant.PluginDescriptorConstants#PLUGIN_DESCRIPTOR_FILE} 的位置，并将其作为内置插件加载。</p>
 */
public class ClasspathPluginLoader implements PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(ClasspathPluginLoader.class);
    private final PluginDescriptorParser<Map<String, Object>> descriptorParser = new YamlPluginDescriptorParser();

    @Override
    public boolean canLoad(Path path) {
        // 类路径加载器通常不通过显式路径加载，除非路径在类路径内。
        // 这里默认返回 false，由 installAll 触发自动发现。
        return false;
    }

    /**
     * 类路径加载器不支持通过路径发现单个插件。
     */
    @Override
    public PluginCandidate discover(Path path, SpiRegistry registry) {
        throw new UnsupportedOperationException("ClasspathPluginLoader 不支持通过路径发现插件");
    }

    @Override
    public List<PluginCandidate> discoverAll(SpiRegistry registry) {
        return discoverAll(registry, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 在指定类加载器范围内扫描并发现所有插件候选。
     *
     * @param registry    SPI 注册中心
     * @param classLoader 要扫描的类加载器
     * @return 候选插件列表
     */
    public List<PluginCandidate> discoverAll(SpiRegistry registry, ClassLoader classLoader) {
        List<PluginCandidate> candidates = new ArrayList<>();
        try {
            // 寻找类路径下所有的描述文件
            Enumeration<URL> resources = classLoader.getResources(PLUGIN_DESCRIPTOR_FILE);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    PluginCandidate candidate = discoverFromUrl(url, registry, classLoader);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } catch (Exception e) {
                    log.warn("从类路径发现插件失败: {}", url, e);
                }
            }
        } catch (IOException e) {
            log.error("扫描类路径插件资源失败", e);
        }
        return candidates;
    }

    private PluginCandidate discoverFromUrl(URL url, SpiRegistry registry, ClassLoader classLoader) throws Exception {
        PluginDescriptor descriptor;
        try (java.io.InputStream is = url.openStream()) {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> raw = yaml.load(is);
            descriptor = descriptorParser.parse(raw);
        }

        java.nio.file.Path path = null;
        try {
            path = java.nio.file.Paths.get(url.toURI()).getParent();
        } catch (Exception ignored) {}

        return new PluginCandidate(descriptor.id(), descriptor, path, this, SourceType.CLASSPATH);
    }

    @Override
    public LoadedPlugin load(PluginCandidate candidate, SpiRegistry registry) {
        PluginDescriptor descriptor = candidate.descriptor();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        Plugin plugin = null;
        try {
            if (descriptor.hasEntryPoint()) {
                Class<?> pluginClass = classLoader.loadClass(descriptor.mainClass());
                if (Plugin.class.isAssignableFrom(pluginClass)) {
                    plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
                }
            }
        } catch (Exception e) {
            throw new com.nexusadmin.plugin.exception.PluginLoadException("加载类路径插件失败: " + descriptor.id(), e);
        }

        return new LoadedPlugin(descriptor, plugin, classLoader, candidate.sourcePath());
    }
}
