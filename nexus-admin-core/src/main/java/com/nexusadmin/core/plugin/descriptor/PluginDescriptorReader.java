package com.nexusadmin.core.plugin.descriptor;

import java.nio.file.Path;

/**
 * 插件描述文件读取器，负责从路径读取并返回描述对象。
 * <p>隐藏底层解析细节，支持多种描述文件格式。</p>
 */
public interface PluginDescriptorReader {

    /**
     * 从路径读取插件描述。
     *
     * @param pluginPath 插件路径（目录或 JAR）
     * @return 插件描述对象
     * @throws com.nexusadmin.core.exception.DescriptorParseException 当读取或解析失败时抛出
     */
    PluginDescriptor read(Path pluginPath);

    /**
     * 检查是否支持给定路径。
     *
     * @param pluginPath 插件路径
     * @return 如果支持则返回 true
     */
    boolean supports(Path pluginPath);
}
