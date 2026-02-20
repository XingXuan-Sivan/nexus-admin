package com.nexusadmin.core.plugin.descriptor;

import com.nexusadmin.core.exception.DescriptorParseException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 插件描述数据模型（不可变）。
 * <p>代表从 META-INF/plugin.json 解析出的插件元数据。</p>
 */
public final class PluginDescriptor {

    /**
     * 插件ID格式校验正则：以字母开头，可包含字母、数字、点号、横线或下划线。
     * <p>支持类似 com.company.myplugin 的命名空间格式。</p>
     */
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9._\\-]{1,127}$");

    private final String id;
    private final String version;
    private final String name;
    private final String description;
    private final String author;
    private final String mainClass;
    private final String coreVersion;
    private final Map<String, String> dependencies;
    private final Map<String, Object> requires;
    private final Map<String, Object> metadata;

    /**
     * 构造插件描述对象。
     *
     * @param id          插件唯一标识，必须符合 {@link #ID_PATTERN} 格式
     * @param version     插件版本号，不能为空
     * @param name        插件名称，可为空
     * @param description 插件描述，可为空
     * @param author      插件作者，可为空
     * @param mainClass   插件入口类全限定名，可为空
     * @param coreVersion 支持的核心版本范围，可为空
     * @param dependencies 依赖的其他插件列表，可为空
     * @param requires    运行环境要求，可为空
     * @param metadata    其他扩展元数据，可为空
     * @throws DescriptorParseException 当 id 或 version 不合法时抛出
     */
    public PluginDescriptor(String id,
                            String version,
                            String name,
                            String description,
                            String author,
                            String mainClass,
                            String coreVersion,
                            Map<String, String> dependencies,
                            Map<String, Object> requires,
                            Map<String, Object> metadata) {
        validate(id, version);
        this.id = id;
        this.version = version;
        this.name = (name == null) ? "" : name.trim();
        this.description = (description == null) ? "" : description.trim();
        this.author = (author == null) ? "" : author.trim();
        this.mainClass = (mainClass == null) ? "" : mainClass.trim();
        this.coreVersion = (coreVersion == null) ? "" : coreVersion.trim();
        this.dependencies = (dependencies == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(dependencies));
        this.requires = (requires == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(requires));
        this.metadata = (metadata == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    /**
     * 验证插件ID和版本号的合法性。
     *
     * @param id      插件ID
     * @param version 插件版本号
     * @throws DescriptorParseException 当验证失败时抛出
     */
    private void validate(String id, String version) {
        if (id == null || id.isBlank()) {
            throw new DescriptorParseException("插件ID不能为空");
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new DescriptorParseException("插件ID格式非法: " + id + "（必须匹配正则: " + ID_PATTERN.pattern() + "）");
        }
        if (version == null || version.isBlank()) {
            throw new DescriptorParseException("插件版本号不能为空");
        }
    }

    /**
     * 获取插件唯一标识。
     *
     * @return 插件ID
     */
    public String id() { return id; }

    /**
     * 获取插件版本号。
     *
     * @return 版本号
     */
    public String version() { return version; }

    /**
     * 获取插件名称。
     *
     * @return 插件名称，可能为空字符串
     */
    public String name() { return name; }

    /**
     * 获取插件描述。
     *
     * @return 插件描述，可能为空字符串
     */
    public String description() { return description; }

    /**
     * 获取插件作者。
     *
     * @return 作者信息，可能为空字符串
     */
    public String author() { return author; }

    /**
     * 获取插件入口类全限定名。
     *
     * @return 入口类名，可能为空字符串
     */
    public String mainClass() { return mainClass; }

    /**
     * 获取支持的核心版本范围。
     *
     * @return 核心版本范围，如 ^1.0.0，可能为空字符串
     */
    public String coreVersion() { return coreVersion; }

    /**
     * 获取依赖的其他插件列表。
     *
     * @return 依赖插件Map，key为插件ID，value为版本范围，不可变
     */
    public Map<String, String> dependencies() { return dependencies; }

    /**
     * 获取运行环境要求。
     *
     * @return 环境要求Map，不可变
     */
    public Map<String, Object> requires() { return requires; }

    /**
     * 获取插件的其他扩展元数据。
     *
     * @return 元数据 Map，不可变
     */
    public Map<String, Object> metadata() { return metadata; }

    /**
     * 判断插件是否包含入口类。
     *
     * @return 如果 mainClass 不为空则返回 true
     */
    public boolean hasEntryPoint() {
        return !mainClass.isBlank();
    }

    /**
     * 判断插件是否有名称。
     *
     * @return 如果 name 不为空则返回 true
     */
    public boolean hasName() {
        return !name.isBlank();
    }

    /**
     * 判断插件是否有依赖。
     *
     * @return 如果 dependencies 不为空则返回 true
     */
    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }
}
