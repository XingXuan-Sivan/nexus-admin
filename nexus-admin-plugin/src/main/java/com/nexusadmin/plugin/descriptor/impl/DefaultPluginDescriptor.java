package com.nexusadmin.plugin.descriptor.impl;

import com.nexusadmin.plugin.descriptor.PluginDescriptor;
import com.nexusadmin.plugin.exception.PluginDescriptorException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件描述的默认实现类，保持不可变性。
 */
public final class DefaultPluginDescriptor implements PluginDescriptor {
    private final String id;
    private final String version;
    private final String mainClass;
    private final List<String> provides;
    private final Map<String, Object> metadata;
    private final int priority;

    /**
     * 构造默认插件描述对象。
     *
     * @param id        插件唯一标识
     * @param version   插件版本号
     * @param mainClass 插件入口类全限定名
     * @param provides  插件声明提供的能力标识集合
     * @param metadata  其他元数据
     * @param priority  加载优先级
     */
    public DefaultPluginDescriptor(String id,
                                   String version,
                                   String mainClass,
                                   List<String> provides,
                                   Map<String, Object> metadata,
                                   int priority) {
        if (id == null || id.isBlank()) {
            throw new PluginDescriptorException("插件ID不能为空");
        }
        if (version == null || version.isBlank()) {
            throw new PluginDescriptorException("插件版本号不能为空");
        }
        this.id = id;
        this.version = version;
        this.mainClass = mainClass == null ? "" : mainClass.trim();
        this.provides = provides == null ? List.of() : List.copyOf(provides);
        this.metadata = metadata == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
        this.priority = priority;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String mainClass() {
        return mainClass;
    }

    @Override
    public List<String> provides() {
        return provides;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public int priority() {
        return priority;
    }
}
