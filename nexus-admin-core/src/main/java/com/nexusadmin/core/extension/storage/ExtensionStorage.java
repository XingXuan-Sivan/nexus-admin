package com.nexusadmin.core.extension.storage;

import com.nexusadmin.core.extension.ExtensionMetadata;

import java.util.List;

/**
 * 扩展索引存储接口。
 * <p>负责从指定类加载器中读取扩展索引，返回扩展实现类元数据列表。</p>
 * <p>该接口支持多实现、可配置、可组合，允许从不同来源（如 META-INF/extensions.idx、
 * META-INF/services 等）加载扩展信息。</p>
 */
public interface ExtensionStorage {

    /**
     * 从指定类加载器中加载扩展元数据列表。
     *
     * @param classLoader 类加载器
     * @return 扩展元数据列表，不会为 null
     */
    List<ExtensionMetadata> loadExtensions(ClassLoader classLoader);

    /**
     * 判断该存储实现是否支持给定的类加载器。
     * <p>默认实现始终返回 true，子类可根据需要覆盖。</p>
     *
     * @param classLoader 类加载器
     * @return 是否支持
     */
    default boolean supports(ClassLoader classLoader) {
        return true;
    }
}
