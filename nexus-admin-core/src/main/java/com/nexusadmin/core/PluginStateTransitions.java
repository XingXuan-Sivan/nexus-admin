package com.nexusadmin.core;

import java.util.*;

/**
 * 插件状态迁移映射表。
 * <p>定义所有合法的插件状态迁移路径，用于状态机校验。</p>
 */
public final class PluginStateTransitions {

    /**
     * 状态迁移映射表。
     * <p>键为源状态，值为该源状态允许迁移到的目标状态集合。</p>
     */
    private static final Map<PluginState, Set<PluginState>> TRANSITIONS = initTransitions();

    /**
     * 初始化状态迁移映射表。
     *
     * @return 不可变的状态迁移映射表
     */
    private static Map<PluginState, Set<PluginState>> initTransitions() {
        Map<PluginState, Set<PluginState>> transitions = new EnumMap<>(PluginState.class);
        
        // 启动阶段迁移
        transitions.put(PluginState.DISCOVERED, EnumSet.of(PluginState.RESOLVED, PluginState.FAILED));
        transitions.put(PluginState.RESOLVED, EnumSet.of(PluginState.LOADED, PluginState.FAILED));
        transitions.put(PluginState.LOADED, EnumSet.of(PluginState.INITIALIZED, PluginState.FAILED));
        transitions.put(PluginState.INITIALIZED, EnumSet.of(PluginState.STARTING, PluginState.DISABLED, PluginState.FAILED));
        
        // 运行阶段迁移
        transitions.put(PluginState.STARTING, EnumSet.of(PluginState.ACTIVE, PluginState.FAILED));
        transitions.put(PluginState.ACTIVE, EnumSet.of(PluginState.STOPPING, PluginState.UPGRADING, PluginState.DISABLED, PluginState.FAILED));
        
        // 停止阶段迁移
        transitions.put(PluginState.STOPPING, EnumSet.of(PluginState.STOPPED, PluginState.FAILED));
        transitions.put(PluginState.STOPPED, EnumSet.of(PluginState.STARTING, PluginState.DISABLED, PluginState.UNLOADED, PluginState.FAILED));
        
        // 禁用状态迁移
        transitions.put(PluginState.DISABLED, EnumSet.of(PluginState.STARTING, PluginState.UNLOADED));
        
        // 升级状态迁移
        transitions.put(PluginState.UPGRADING, EnumSet.of(PluginState.ACTIVE, PluginState.FAILED));
        
        // 终态和异常状态迁移
        transitions.put(PluginState.UNLOADED, EnumSet.noneOf(PluginState.class));
        transitions.put(PluginState.FAILED, EnumSet.of(PluginState.UNLOADED));
        
        return Collections.unmodifiableMap(transitions);
    }

    /**
     * 检查状态迁移是否合法。
     *
     * @param from 源状态
     * @param to   目标状态
     * @return 如果允许迁移返回 true，否则返回 false
     */
    public static boolean canTransition(PluginState from, PluginState to) {
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    /**
     * 获取指定状态允许迁移到的目标状态集合。
     *
     * @param from 源状态
     * @return 允许迁移到的目标状态集合，如果源状态不存在则返回空集合
     */
    public static Set<PluginState> getAllowedTransitions(PluginState from) {
        return TRANSITIONS.getOrDefault(from, Collections.emptySet());
    }

    /**
     * 私有构造函数，防止实例化。
     */
    private PluginStateTransitions() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
}
