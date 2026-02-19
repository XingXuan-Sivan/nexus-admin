package com.nexusadmin.core.extension.finder;

import com.nexusadmin.core.extension.ExtensionPoint;
import com.nexusadmin.core.extension.ExtensionWrapper;

import java.util.List;

/**
 * 扩展发现器接口。
 * <p>负责从指定类加载器中发现扩展点及其实现。</p>
 * <p>该接口支持多实现、可配置、可组合，允许从不同来源发现扩展实现。</p>
 */
public interface ExtensionFinder {

    /**
     * 查找指定扩展点类型的所有实现。
     * <p>返回的包装器列表按优先级降序排列。</p>
     *
     * @param pointType 扩展点接口类型
     * @param classLoader 类加载器
     * @param pluginId 插件ID，可为 null
     * @param <T> 扩展点类型
     * @return 扩展包装器列表，不会为 null
     */
    <T extends ExtensionPoint> List<ExtensionWrapper<T>> find(
            Class<T> pointType,
            ClassLoader classLoader,
            String pluginId);

    /**
     * 查找指定扩展点类型的所有实现（不带插件ID）。
     *
     * @param pointType 扩展点接口类型
     * @param classLoader 类加载器
     * @param <T> 扩展点类型
     * @return 扩展包装器列表
     */
    default <T extends ExtensionPoint> List<ExtensionWrapper<T>> find(
            Class<T> pointType,
            ClassLoader classLoader) {
        return find(pointType, classLoader, null);
    }
}
