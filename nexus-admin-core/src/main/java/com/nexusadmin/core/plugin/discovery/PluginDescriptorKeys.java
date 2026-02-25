package com.nexusadmin.core.plugin.discovery;

/**
 * 插件描述文件字段常量定义。
 * <p>定义 plugin.json 中所有标准字段的键名，便于统一管理和避免硬编码。</p>
 */
public final class PluginDescriptorKeys {

    private PluginDescriptorKeys() {
        // 工具类禁止实例化
    }

    /**
     * 必填字段：插件唯一标识。
     * <p>格式类似 com.company.myplugin</p>
     */
    public static final String KEY_ID = "id";

    /**
     * 必填字段：插件版本号。
     */
    public static final String KEY_VERSION = "version";

    /**
     * 可选字段：插件名称。
     */
    public static final String KEY_NAME = "name";

    /**
     * 可选字段：插件描述。
     */
    public static final String KEY_DESCRIPTION = "description";

    /**
     * 可选字段：插件作者。
     */
    public static final String KEY_AUTHOR = "author";

    /**
     * 可选字段：插件入口类全限定名。
     */
    public static final String KEY_MAIN_CLASS = "mainClass";

    /**
     * 可选字段：支持的核心版本范围。
     * <p>如 ^1.0.0 表示兼容 1.x.x 版本</p>
     */
    public static final String KEY_CORE_VERSION = "coreVersion";

    /**
     * 可选字段：依赖的其他插件列表。
     * <p>值为 Map 结构，key 为插件ID，value 为版本范围</p>
     */
    public static final String KEY_DEPENDENCIES = "dependencies";

    /**
     * 可选字段：运行环境要求。
     */
    public static final String KEY_REQUIRES = "requires";

    /**
     * 标准字段集合，用于区分标准字段和元数据字段。
     */
    public static final String[] STANDARD_KEYS = {
            KEY_ID,
            KEY_VERSION,
            KEY_NAME,
            KEY_DESCRIPTION,
            KEY_AUTHOR,
            KEY_MAIN_CLASS,
            KEY_CORE_VERSION,
            KEY_DEPENDENCIES,
            KEY_REQUIRES
    };

    /**
     * 插件描述文件名称。
     */
    public static final String DESCRIPTOR_FILE_NAME = "plugin.json";

    /**
     * 插件描述文件在 JAR/打包目录中的标准路径。
     */
    public static final String DESCRIPTOR_PATH = "META-INF/" + DESCRIPTOR_FILE_NAME;

    /**
     * 插件描述文件在开发环境（Maven 源码目录）中的路径。
     */
    public static final String DESCRIPTOR_DEV_PATH = "src/main/resources/" + DESCRIPTOR_PATH;
}
