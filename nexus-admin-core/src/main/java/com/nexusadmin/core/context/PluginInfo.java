package com.nexusadmin.core.context;

import com.nexusadmin.core.plugin.discovery.PluginDescriptor;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 插件静态元数据，封装插件的基本信息。
 * <p>包含插件描述符、类加载器和物理路径等不可变信息。</p>
 */
public final class PluginInfo {

    private final PluginDescriptor descriptor;
    private final ClassLoader classLoader;
    private final Path physicalPath;

    /**
     * 构造插件信息对象。
     *
     * @param descriptor   插件描述符
     * @param classLoader  插件类加载器
     * @param physicalPath 插件物理路径
     */
    public PluginInfo(PluginDescriptor descriptor, ClassLoader classLoader, Path physicalPath) {
        this.descriptor = Objects.requireNonNull(descriptor, "插件描述符不能为空");
        this.classLoader = Objects.requireNonNull(classLoader, "类加载器不能为空");
        this.physicalPath = physicalPath;
    }

    /**
     * 获取插件唯一标识。
     *
     * @return 插件ID
     */
    public String id() {
        return descriptor.id();
    }

    /**
     * 获取插件版本号。
     *
     * @return 版本号
     */
    public String version() {
        return descriptor.version();
    }

    /**
     * 获取插件描述符。
     *
     * @return 插件描述符
     */
    public PluginDescriptor descriptor() {
        return descriptor;
    }

    /**
     * 获取插件类加载器。
     *
     * @return 类加载器
     */
    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * 获取插件物理路径。
     *
     * @return 物理路径，可能为 null
     */
    public Path physicalPath() {
        return physicalPath;
    }
}
