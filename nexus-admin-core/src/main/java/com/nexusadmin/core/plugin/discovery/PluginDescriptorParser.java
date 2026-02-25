package com.nexusadmin.core.plugin.discovery;

import java.nio.file.Path;

/**
 * 插件描述解析器接口，负责从插件路径解析出插件描述信息。
 * <p>专注解析逻辑，支持多种描述文件格式。</p>
 */
public interface PluginDescriptorParser {

    /**
     * 检查是否支持解析给定路径。
     *
     * @param pluginPath 插件路径（目录或 JAR）
     * @return 如果支持则返回 true
     */
    boolean supports(Path pluginPath);

    /**
     * 从路径解析插件描述。
     *
     * @param pluginPath 插件路径（目录或 JAR）
     * @return 解析后的插件描述对象
     */
    PluginDescriptor parse(Path pluginPath);
}
