package com.nexusadmin.core.config.event;

/**
 * 配置变更监听器接口。
 * <p>插件可实现此接口监听配置变更事件，实现配置热更新。</p>
 */
@FunctionalInterface
public interface ConfigListener {

    /**
     * 当配置发生变更时调用。
     *
     * @param event 配置变更事件
     */
    void onConfigChanged(ConfigChangedEvent event);

    /**
     * 获取监听器感兴趣的作用域。
     * <p>返回 null 或空字符串表示监听所有作用域。</p>
     *
     * @return 作用域前缀，如 "plugin.order-plugin"
     */
    default String interestedScope() {
        return null;
    }
}
