package com.nexusadmin.core.plugin.loader;

import com.nexusadmin.core.plugin.descriptor.PluginDescriptor;

import java.nio.file.Path;

/**
 * 插件元数据对象，代表在发现阶段识别到的插件信息，尚未执行安装。
 *
 * @param pluginId   插件唯一标识
 * @param descriptor 插件描述信息
 * @param sourcePath 插件物理来源路径
 * @param loader     发现该插件的加载器，后续也将由其负责加载
 * @param sourceType 插件来源类型
 */
public record PluginMetadata(
        String pluginId,
        PluginDescriptor descriptor,
        Path sourcePath,
        PluginLoader loader,
        SourceType sourceType
) {
}
