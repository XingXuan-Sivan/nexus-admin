package com.nexusadmin.plugin.descriptor;

import java.nio.file.Path;

/**
 * 插件描述解析器接口，负责从指定格式的数据源中解析出插件描述信息，或直接从物理路径加载。
 *
 * @param <S> 原始数据源类型（如 Map, String 等）
 */
public interface PluginDescriptorParser<S> {
    /**
     * 执行解析操作。
     *
     * @param source 原始数据源
     * @return 解析后的插件描述对象
     */
    PluginDescriptor parse(S source);

    /**
     * 从指定路径加载并解析插件描述。
     *
     * @param path 插件所在路径（目录或 JAR）
     * @return 解析后的插件描述对象
     */
    PluginDescriptor load(Path path);
}
