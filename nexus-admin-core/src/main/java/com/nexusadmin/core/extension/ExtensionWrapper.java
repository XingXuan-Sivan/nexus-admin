package com.nexusadmin.core.extension;

import com.nexusadmin.api.extension.ExtensionPoint;

import java.util.Objects;

/**
 * 扩展实现包装类。
 * <p>封装扩展实现实例及其元信息，用于统一管理和排序。</p>
 *
 * @param <T> 扩展点类型
 * @author NexusAdmin
 * @since 1.0.0
 */
public final class ExtensionWrapper<T extends ExtensionPoint> implements Comparable<ExtensionWrapper<?>> {

    private final T instance;
    private final Class<T> pointType;
    private final String pluginId;
    private final int priority;
    private final boolean enabled;
    private final String name;
    private final String description;
    private final long sequence;

    private static final java.util.concurrent.atomic.AtomicLong SEQUENCE_GENERATOR =
            new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * 构造扩展包装器。
     *
     * @param instance    扩展实现实例
     * @param pointType   扩展点类型
     * @param pluginId    所属插件ID
     * @param priority    优先级
     * @param enabled     是否启用
     * @param name        扩展名称
     * @param description 扩展描述
     */
    public ExtensionWrapper(T instance,
                            Class<T> pointType,
                            String pluginId,
                            int priority,
                            boolean enabled,
                            String name,
                            String description) {
        this.instance = Objects.requireNonNull(instance, "扩展实现实例不能为空");
        this.pointType = Objects.requireNonNull(pointType, "扩展点类型不能为空");
        this.pluginId = pluginId != null ? pluginId : "";
        this.priority = priority;
        this.enabled = enabled;
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
    }

    /**
     * 获取扩展实现实例。
     *
     * @return 扩展实现实例
     */
    public T getInstance() {
        return instance;
    }

    /**
     * 获取扩展点类型。
     *
     * @return 扩展点类型
     */
    public Class<T> getPointType() {
        return pointType;
    }

    /**
     * 获取所属插件ID。
     *
     * @return 插件ID，可能为空字符串
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取优先级。
     *
     * @return 优先级数值
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 判断是否启用。
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取扩展名称。
     *
     * @return 扩展名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取扩展描述。
     *
     * @return 扩展描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取注册序列号。
     * <p>用于相同优先级时的排序，保证后注册者优先。</p>
     *
     * @return 序列号
     */
    public long getSequence() {
        return sequence;
    }

    @Override
    public int compareTo(ExtensionWrapper<?> other) {
        // 先按优先级降序，再按序列号降序（后注册者优先）
        int priorityCompare = Integer.compare(other.priority, this.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return Long.compare(other.sequence, this.sequence);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionWrapper<?> that = (ExtensionWrapper<?>) o;
        return sequence == that.sequence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence);
    }

    @Override
    public String toString() {
        return "ExtensionWrapper{" +
                "pointType=" + pointType.getName() +
                ", pluginId='" + pluginId + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                ", name='" + name + '\'' +
                ", instance=" + instance.getClass().getName() +
                '}';
    }
}
