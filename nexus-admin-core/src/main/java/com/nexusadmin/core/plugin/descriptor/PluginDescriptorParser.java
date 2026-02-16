package com.nexusadmin.core.plugin.descriptor;

import com.nexusadmin.api.PluginDescriptor;
import com.nexusadmin.core.registry.Composable;

/**
 * 插件描述解析器接口，负责从指定格式的数据源中解析出插件描述信息。
 * <p>专注解析逻辑，不涉及路径处理。</p>
 * <p>实现 {@link Composable} 以支持注册中心统一管理。</p>
 *
 * @param <S> 原始数据源类型（如 InputStream, String 等）
 */
public interface PluginDescriptorParser<S> extends Composable {

    /**
     * 执行解析操作。
     *
     * @param source 原始数据源
     * @return 解析后的插件描述对象
     */
    PluginDescriptor parse(S source);
}
