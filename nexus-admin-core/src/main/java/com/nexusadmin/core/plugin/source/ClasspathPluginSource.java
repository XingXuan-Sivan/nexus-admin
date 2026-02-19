package com.nexusadmin.core.plugin.source;

import com.nexusadmin.core.plugin.PluginDescriptor;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorParser;
import com.nexusadmin.core.plugin.loader.CandidatePlugin;
import com.nexusadmin.core.plugin.loader.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static com.nexusadmin.core.plugin.descriptor.PluginDescriptorKeys.DESCRIPTOR_PATH;

/**
 * 类路径插件源，从当前运行环境的类路径中自动探测插件。
 */
public class ClasspathPluginSource implements PluginSource {

    private static final Logger log = LoggerFactory.getLogger(ClasspathPluginSource.class);

    private final PluginDescriptorParser<InputStream> descriptorParser;
    private final ClassLoader classLoader;

    public ClasspathPluginSource(PluginDescriptorParser<InputStream> descriptorParser) {
        this(descriptorParser, Thread.currentThread().getContextClassLoader());
    }

    public ClasspathPluginSource(PluginDescriptorParser<InputStream> descriptorParser, ClassLoader classLoader) {
        this.descriptorParser = descriptorParser;
        this.classLoader = classLoader;
    }

    @Override
    public String sourceType() {
        return "classpath";
    }

    @Override
    public List<CandidatePlugin> scan() {
        List<CandidatePlugin> candidates = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(DESCRIPTOR_PATH);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    CandidatePlugin candidate = createCandidateFromUrl(url);
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

    private CandidatePlugin createCandidateFromUrl(URL url) throws Exception {
        PluginDescriptor descriptor;
        try (InputStream is = url.openStream()) {
            descriptor = descriptorParser.parse(is);
        }

        Path path = null;
        try {
            path = Paths.get(url.toURI()).getParent();
        } catch (Exception ignored) {
            // URL 可能无法转换为路径，忽略
        }

        log.debug("发现类路径插件: {} (来源: {})", descriptor.id(), url);
        return new CandidatePlugin(descriptor.id(), descriptor, path, null, SourceType.CLASSPATH);
    }
}
