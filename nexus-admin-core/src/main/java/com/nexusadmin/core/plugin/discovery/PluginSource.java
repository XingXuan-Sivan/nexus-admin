package com.nexusadmin.core.plugin.discovery;

import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.SourceType;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * 插件源抽象，负责从不同来源发现插件元数据对象。
 * <p>支持本地目录、远程仓库、数据库等多种来源。</p>
 */
public interface PluginSource {

    /**
     * 获取源类型标识。
     *
     * @return 源类型枚举
     */
    SourceType getType();

    /**
     * 获取类路径URL数组，用于创建插件类加载器。
     *
     * @return 类路径URL数组
     */
    URL[] getClasspath();

    /**
     * 获取插件物理路径（如果存在）。
     * <p>某些来源可能没有物理路径（如纯内存加载），返回 null。</p>
     *
     * @return 插件物理路径，可能为 null
     */
    Path getPhysicalPath();

    /**
     * 扫描并返回所有候选插件。
     *
     * @return 候选插件列表，不会返回 null
     */
    List<PluginMetadata> scan();

    /**
     * 是否支持动态刷新（如远程源）。
     *
     * @return 如果支持动态刷新返回 true
     */
    default boolean supportsRefresh() {
        return false;
    }

    /**
     * 刷新插件源，重新扫描。
     * <p>默认实现直接调用 scan()。</p>
     *
     * @return 刷新后的候选插件列表
     */
    default List<PluginMetadata> refresh() {
        return scan();
    }

    /**
     * 是否支持物理卸载。
     *
     * @return 如果支持物理卸载返回 true
     */
    default boolean supportsPhysicalRemoval() {
        return false;
    }

    /**
     * 执行物理卸载（删除文件）。
     *
     * @throws UnsupportedOperationException 如果不支持物理卸载
     */
    default void removePhysically() {
        throw new UnsupportedOperationException("不支持物理卸载");
    }
}
