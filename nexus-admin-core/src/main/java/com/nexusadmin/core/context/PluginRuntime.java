package com.nexusadmin.core.context;

import com.nexusadmin.core.PluginState;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 插件运行时状态访问，提供对插件当前状态的动态查询能力。
 * <p>通过状态供应器实时获取状态，不缓存状态值。</p>
 */
public final class PluginRuntime {

    private final Supplier<PluginState> stateSupplier;

    /**
     * 构造插件运行时对象。
     *
     * @param stateSupplier 状态供应器
     */
    public PluginRuntime(Supplier<PluginState> stateSupplier) {
        this.stateSupplier = Objects.requireNonNull(stateSupplier, "状态供应器不能为空");
    }

    /**
     * 获取插件当前状态。
     * <p>每次调用都返回实时状态，不缓存。</p>
     *
     * @return 当前插件状态
     */
    public PluginState state() {
        return stateSupplier.get();
    }

    /**
     * 检查插件是否处于活跃状态。
     *
     * @return 如果插件处于 ACTIVE 状态返回 true
     */
    public boolean isActive() {
        return stateSupplier.get() == PluginState.ACTIVE;
    }

    /**
     * 检查插件是否已禁用。
     *
     * @return 如果插件处于 DISABLED 状态返回 true
     */
    public boolean isDisabled() {
        return stateSupplier.get() == PluginState.DISABLED;
    }

    /**
     * 检查插件是否正在启动中。
     *
     * @return 如果插件处于 STARTING 状态返回 true
     */
    public boolean isStarting() {
        return stateSupplier.get() == PluginState.STARTING;
    }

    /**
     * 检查插件是否已初始化。
     *
     * @return 如果插件处于 INITIALIZED 或更高状态返回 true
     */
    public boolean isInitialized() {
        PluginState state = stateSupplier.get();
        return state == PluginState.INITIALIZED
                || state == PluginState.STARTING
                || state == PluginState.ACTIVE
                || state == PluginState.STOPPING
                || state == PluginState.STOPPED
                || state == PluginState.DISABLED;
    }
}
