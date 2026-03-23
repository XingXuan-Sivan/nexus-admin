package com.nexusadmin.core.config.event;

import com.nexusadmin.core.event.Event;
import com.nexusadmin.core.event.EventScope;

import java.util.Objects;

/**
 * 配置变更事件，当配置值发生变化时发布。
 * <p>插件可监听此事件实现配置热更新。</p>
 * <p>配置变更事件使用平台作用域，表示由平台核心发布。</p>
 */
public class ConfigChangedEvent extends Event {

    /**
     * 配置键名。
     */
    private final String key;

    /**
     * 新配置值。
     */
    private final String value;

    /**
     * 旧配置值，可能为 null。
     */
    private final String oldValue;

    /**
     * 配置作用域字符串表示（用于配置中心内部使用）。
     */
    private final String configScope;

    /**
     * 构造配置变更事件。
     * <p>使用平台作用域，表示这是平台核心发布的配置变更事件。</p>
     *
     * @param configScope 配置作用域字符串
     * @param key         配置键名
     * @param value       新配置值
     * @param oldValue    旧配置值
     */
    public ConfigChangedEvent(String configScope, String key, String value, String oldValue) {
        super(EventScope.platform());
        this.configScope = Objects.requireNonNull(configScope, "配置作用域不能为空");
        this.key = Objects.requireNonNull(key, "配置键名不能为空");
        this.value = value;
        this.oldValue = oldValue;
    }

    /**
     * 获取配置作用域字符串。
     *
     * @return 配置作用域字符串
     */
    public String configScope() {
        return configScope;
    }

    /**
     * 获取配置键名。
     *
     * @return 键名
     */
    public String key() {
        return key;
    }

    /**
     * 获取新配置值。
     *
     * @return 新值
     */
    public String value() {
        return value;
    }

    /**
     * 获取旧配置值。
     *
     * @return 旧值，可能为 null
     */
    public String oldValue() {
        return oldValue;
    }

    /**
     * 获取完整配置键（作用域 + 键名）。
     *
     * @return 完整键名
     */
    public String fullKey() {
        return configScope + "." + key;
    }

    @Override
    public String toString() {
        return String.format("ConfigChangedEvent[scope=%s, key=%s, value=%s]", configScope, key, value);
    }
}
