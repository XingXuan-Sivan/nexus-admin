package com.nexusadmin.core.extension;

import com.nexusadmin.core.event.Event;
import com.nexusadmin.core.event.EventScope;

import java.util.Objects;

/**
 * 扩展点变更事件，当 ExtensionRegistry 中有扩展注册/注销时发布。
 * <p>扩展点变更事件使用平台作用域，表示由平台核心发布。</p>
 */
public final class ExtensionChangedEvent extends Event {

    /**
     * 变更类型。
     */
    public enum ChangeType {

        /**
         * 扩展已注册。
         */
        REGISTERED,

        /**
         * 扩展已注销。
         */
        UNREGISTERED,

        /**
         * 扩展点已清空。
         */
        CLEARED
    }

    /**
     * 变更的扩展点类型。
     */
    private final Class<? extends ExtensionPoint> pointType;

    /**
     * 变更类型。
     */
    private final ChangeType changeType;

    /**
     * 变更的实现实例，CLEARED 时为 null。
     */
    private final ExtensionPoint implementation;

    /**
     * 构造扩展点变更事件。
     *
     * @param pointType      变更的扩展点类型
     * @param changeType     变更类型
     * @param implementation 变更的实现实例，CLEARED 时可为 null
     */
    public ExtensionChangedEvent(Class<? extends ExtensionPoint> pointType,
                                 ChangeType changeType,
                                 ExtensionPoint implementation) {
        super(EventScope.platform());
        this.pointType = Objects.requireNonNull(pointType, "扩展点类型不能为空");
        this.changeType = Objects.requireNonNull(changeType, "变更类型不能为空");
        this.implementation = implementation;
    }

    /**
     * 获取变更的扩展点类型。
     *
     * @return 扩展点类型
     */
    public Class<? extends ExtensionPoint> pointType() {
        return pointType;
    }

    /**
     * 获取变更类型。
     *
     * @return 变更类型
     */
    public ChangeType changeType() {
        return changeType;
    }

    /**
     * 获取变更的实现实例。
     *
     * @return 变更的实现实例，CLEARED 时返回 null
     */
    public ExtensionPoint implementation() {
        return implementation;
    }

    @Override
    public String toString() {
        return String.format("ExtensionChangedEvent[pointType=%s, changeType=%s]",
                pointType.getSimpleName(), changeType);
    }
}
