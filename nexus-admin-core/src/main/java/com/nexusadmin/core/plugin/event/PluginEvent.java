package com.nexusadmin.core.plugin.event;

import com.nexusadmin.core.event.Event;
import com.nexusadmin.core.event.EventScope;

import java.time.Instant;

/**
 * 插件相关事件的基类。
 * <p>所有插件相关事件都需要继承此类，用于关联插件ID。</p>
 * <p>插件事件统一使用平台作用域，表示由平台核心发布。</p>
 */
public abstract class PluginEvent extends Event {

    /**
     * 关联的插件ID。
     */
    private final String pluginId;

    /**
     * 构造插件事件。
     * <p>使用平台作用域，表示这是平台核心发布的生命周期事件。</p>
     *
     * @param pluginId 插件唯一标识
     */
    protected PluginEvent(String pluginId) {
        super(EventScope.platform());
        this.pluginId = pluginId;
    }

    /**
     * 构造插件事件，指定ID和时间戳。
     * <p>使用平台作用域，表示这是平台核心发布的生命周期事件。</p>
     *
     * @param id        事件唯一标识
     * @param timestamp 事件发生时间戳
     * @param pluginId  插件唯一标识
     */
    protected PluginEvent(String id, Instant timestamp, String pluginId) {
        super(id, timestamp, EventScope.platform());
        this.pluginId = pluginId;
    }

    /**
     * 获取关联的插件ID。
     *
     * @return 插件唯一标识，可能为 null
     */
    public String pluginId() {
        return pluginId;
    }
}
