package com.nexusadmin.core.plugin.event;

import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.time.Instant;

/**
 * 插件升级事件。
 *
 * <p>
 * 支持冷升级与热升级双实例模式。
 * </p>
 */
public final class PluginUpgradeEvent extends PluginEvent {

    /**
     * 升级模式枚举。
     */
    public enum Mode {
        /**
         * 冷升级（停止旧版本，启动新版本）。
         */
        COLD,

        /**
         * 热升级（双实例运行，无缝切换）。
         */
        HOT
    }

    private final Mode mode;
    private final PluginWrapper oldInstance;
    private final PluginWrapper newInstance;
    private final Instant occurredAt;

    /**
     * 构造插件升级事件。
     *
     * @param mode        升级模式
     * @param oldInstance 旧版本插件实例
     * @param newInstance 新版本插件实例
     */
    public PluginUpgradeEvent(
            Mode mode,
            PluginWrapper oldInstance,
            PluginWrapper newInstance) {

        super(oldInstance.getPluginId());

        this.mode = mode;
        this.oldInstance = oldInstance;
        this.newInstance = newInstance;
        this.occurredAt = Instant.now();
    }

    /**
     * 获取升级模式。
     *
     * @return 升级模式
     */
    public Mode mode() {
        return mode;
    }

    /**
     * 获取旧版本插件实例。
     *
     * @return 旧版本插件实例
     */
    public PluginWrapper oldInstance() {
        return oldInstance;
    }

    /**
     * 获取新版本插件实例。
     *
     * @return 新版本插件实例
     */
    public PluginWrapper newInstance() {
        return newInstance;
    }

    /**
     * 获取事件发生时间。
     *
     * @return 事件发生时间
     */
    public Instant occurredAt() {
        return occurredAt;
    }
}
