package com.nexusadmin.plugin.constant;

/**
 * 插件描述模块常量定义。
 */
public interface PluginDescriptorConstants {
    /**
     * 插件描述文件名。
     */
    String PLUGIN_DESCRIPTOR_FILE = "plugin.yml";

    /**
     * 插件标识字段 Key。
     */
    String KEY_ID = "id";

    /**
     * 插件版本号字段 Key。
     */
    String KEY_VERSION = "version";

    /**
     * 插件入口类字段 Key。
     */
    String KEY_MAIN_CLASS = "mainClass";

    /**
     * 插件能力声明字段 Key。
     */
    String KEY_PROVIDES = "provides";

    /**
     * 插件加载优先级。
     */
    String KEY_PRIORITY = "priority";
}
