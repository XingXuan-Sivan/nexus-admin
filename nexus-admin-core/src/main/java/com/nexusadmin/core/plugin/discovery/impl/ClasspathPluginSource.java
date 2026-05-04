package com.nexusadmin.core.plugin.discovery.impl;

import com.nexusadmin.core.plugin.discovery.PluginDescriptor;
import com.nexusadmin.core.plugin.discovery.PluginSource;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.SourceType;
import com.nexusadmin.core.exception.DescriptorParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import static com.nexusadmin.core.plugin.discovery.PluginDescriptorKeys.*;

/**
 * 类路径插件源，从当前运行环境的类路径中自动探测插件。
 */
public class ClasspathPluginSource implements PluginSource {

    private static final Logger log = LoggerFactory.getLogger(ClasspathPluginSource.class);

    private final ClassLoader classLoader;
    private URL[] classpath;
    private Path physicalPath;

    public ClasspathPluginSource() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClasspathPluginSource(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public SourceType getType() {
        return SourceType.CLASSPATH;
    }

    @Override
    public URL[] getClasspath() {
        if (classpath == null) {
            return new URL[0];
        }
        return classpath.clone();
    }

    @Override
    public Path getPhysicalPath() {
        return physicalPath;
    }

    @Override
    public boolean supportsPhysicalRemoval() {
        return false;
    }

    @Override
    public List<PluginMetadata> scan() {
        List<PluginMetadata> candidates = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(DESCRIPTOR_PATH);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    PluginMetadata candidate = createCandidateFromUrl(url);
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

    private PluginMetadata createCandidateFromUrl(URL url) throws Exception {
        PluginDescriptor descriptor;
        try (InputStream is = url.openStream()) {
            descriptor = parseDescriptor(is);
        }

        // 尝试获取物理路径
        this.physicalPath = null;
        try {
            this.physicalPath = Paths.get(url.toURI()).getParent();
        } catch (Exception ignored) {
            // URL 可能无法转换为路径，忽略
        }

        // 设置类路径
        this.classpath = new URL[]{url};

        log.debug("发现类路径插件: {} (来源: {})", descriptor.id(), url);
        return new PluginMetadata(descriptor.id(), descriptor, this);
    }

    private PluginDescriptor parseDescriptor(InputStream source) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            JsonObject json = Json.parse(reader).asObject();

            if (!json.names().contains(KEY_ID) || !json.names().contains(KEY_VERSION)) {
                throw new DescriptorParseException("plugin.json 缺失必填字段: " + KEY_ID + " 或 " + KEY_VERSION);
            }

            String id = json.getString(KEY_ID, "").trim();
            String version = json.getString(KEY_VERSION, "").trim();
            String name = json.getString(KEY_NAME, "").trim();
            String description = json.getString(KEY_DESCRIPTION, "").trim();
            String author = json.getString(KEY_AUTHOR, "").trim();
            String mainClass = json.getString(KEY_MAIN_CLASS, "").trim();
            String coreVersion = json.getString(KEY_CORE_VERSION, "").trim();

            Map<String, String> dependencies = parseDependencies(json.get(KEY_DEPENDENCIES));
            Map<String, Object> requires = parseRequires(json.get(KEY_REQUIRES));

            return new PluginDescriptor(
                    id, version, name, description, author,
                    mainClass, coreVersion, dependencies, requires, Map.of()
            );
        }
    }

    private Map<String, String> parseDependencies(JsonValue value) {
        Map<String, String> deps = new HashMap<>();
        if (value == null || value.isNull()) {
            return deps;
        }
        if (value.isObject()) {
            JsonObject obj = value.asObject();
            for (String key : obj.names()) {
                JsonValue val = obj.get(key);
                if (val.isString()) {
                    deps.put(key, val.asString());
                }
            }
        }
        return deps;
    }

    private Map<String, Object> parseRequires(JsonValue value) {
        Map<String, Object> reqs = new HashMap<>();
        if (value == null || value.isNull()) {
            return reqs;
        }
        if (value.isObject()) {
            JsonObject obj = value.asObject();
            for (String key : obj.names()) {
                reqs.put(key, toJsonValue(obj.get(key)));
            }
        }
        return reqs;
    }

    private Object toJsonValue(JsonValue value) {
        if (value == null || value.isNull()) return null;
        if (value.isString()) return value.asString();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) return value.asDouble();
        if (value.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonValue v : value.asArray()) {
                list.add(toJsonValue(v));
            }
            return list;
        }
        if (value.isObject()) {
            Map<String, Object> map = new HashMap<>();
            for (String k : value.asObject().names()) {
                map.put(k, toJsonValue(value.asObject().get(k)));
            }
            return map;
        }
        return value.toString();
    }
}
