package com.nexusadmin.core.plugin.loader;

import com.nexusadmin.core.plugin.discovery.PluginDescriptor;
import com.nexusadmin.core.plugin.discovery.PluginSource;

/**
 * 插件元数据对象，代表在发现阶段识别到的插件信息，尚未执行安装。
 *
 * @param pluginId   插件唯一标识
 * @param descriptor 插件描述信息
 * @param source     插件来源，包含类型和类路径信息
 */
public record PluginMetadata(
        String pluginId,
        PluginDescriptor descriptor,
        PluginSource source
) {

    /**
     * 获取插件主类名。
     *
     * @return 主类名
     */
    public String getMainClass() {
        return descriptor.mainClass();
    }

    /**
     * 获取插件来源类型。
     *
     * @return 来源类型
     */
    public SourceType sourceType() {
        return source.getType();
    }
}
