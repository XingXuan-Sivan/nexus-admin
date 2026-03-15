package com.nexusadmin.core.config.event;

import com.nexusadmin.core.event.Event;

/**
 * 配置变更事件，当配置值发生变化时发布。
 * <p>插件可监听此事件实现配置热更新。</p>
 */
public class ConfigChangedEvent extends Event {

    /**
     * 配置作用域。
     */
    private final String scope;

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
     * 构造配置变更事件。
     *
     * @param scope    配置作用域
     * @param key      配置键名
     * @param value    新配置值
     * @param oldValue 旧配置值
     */
    public ConfigChangedEvent(String scope, String key, String value, String oldValue) {
        this.scope = scope;
        this.key = key;
        this.value = value;
        this.oldValue = oldValue;
    }

    /**
     * 获取配置作用域。
     *
     * @return 作用域
     */
    public String scope() {
        return scope;
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
        return scope + "." + key;
    }

    @Override
    public String toString() {
        return String.format("ConfigChangedEvent[scope=%s, key=%s, value=%s]", scope, key, value);
    }
}
