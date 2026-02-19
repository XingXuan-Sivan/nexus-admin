package com.nexusadmin.core.plugin.descriptor;

import com.nexusadmin.core.registry.Composable;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 插件描述文件查找策略接口。
 * <p>用于在不同环境（开发环境、打包环境）中定位插件描述文件。</p>
 * <p>实现 {@link Composable} 以支持注册中心统一管理。</p>
 */
public interface PluginDescriptorFinder extends Composable {

    /**
     * 尝试查找给定插件路径下的描述文件路径。
     *
     * @param pluginPath 插件路径（目录或 JAR）
     * @return 描述文件的绝对路径，如果未找到则返回 empty
     */
    Optional<Path> find(Path pluginPath);

    /**
     * 获取该查找器的优先级，数值越小优先级越高。
     *
     * @return 优先级数值
     */
    int priority();
}
