package com.nexusadmin.core.extension;

import com.nexusadmin.core.event.EventBus;

import java.util.List;
import java.util.Optional;

/**
 * 扩展点通用消费者模板。
 * <p>
 * 提供缓存 + 事件驱动失效的动态解析能力。
 * 所有需要消费扩展点的组件都应通过此类获取扩展实现，而非直接调用 ExtensionRegistry。
 *
 * @param <T> 扩展点接口泛型
 */
public class ExtensionConsumer<T extends ExtensionPoint> {

    private final Class<T> pointType;
    private final ExtensionRegistry registry;

    /**
     * 缓存的所有实现列表（按优先级降序）。
     */
    private volatile List<T> cachedAll;

    /**
     * 缓存的最高优先级实现。
     */
    private volatile T cachedTop;

    /**
     * 构造扩展点消费者。
     * <p>构造时即订阅扩展变更事件，当关联扩展点发生注册/注销/清空时自动失效缓存。</p>
     *
     * @param pointType 扩展点接口类型
     * @param registry  扩展注册中心
     * @param eventBus  事件总线，用于监听扩展变更事件
     */
    public ExtensionConsumer(Class<T> pointType, ExtensionRegistry registry, EventBus eventBus) {
        this.pointType = pointType;
        this.registry = registry;

        // 监听扩展注册变更事件，自动失效缓存
        eventBus.subscribe(ExtensionChangedEvent.class, event -> {
            if (event.pointType().equals(pointType)) {
                invalidateCache();
            }
        });
    }

    /**
     * 获取最高优先级实现。
     * <p>当存在多个相同优先级的实现时，返回最后注册的实现。</p>
     *
     * @return 扩展实现，可为空
     */
    public Optional<T> get() {
        T top = cachedTop;
        if (top == null) {
            synchronized (this) {
                top = cachedTop;
                if (top == null) {
                    List<T> all = resolveAll();
                    if (!all.isEmpty()) {
                        top = all.get(0);
                    }
                    cachedTop = top;
                }
            }
        }
        return Optional.ofNullable(top);
    }

    /**
     * 获取所有实现（按优先级降序）。
     * <p>返回的列表为不可变副本，调用方可安全遍历。</p>
     *
     * @return 所有实现组成的不可变列表
     */
    public List<T> getAll() {
        List<T> all = cachedAll;
        if (all == null) {
            synchronized (this) {
                all = cachedAll;
                if (all == null) {
                    all = resolveAll();
                    cachedAll = all;
                }
            }
        }
        return all;
    }

    /**
     * 从注册中心解析所有实现，返回不可变列表副本。
     *
     * @return 不可变实现列表
     */
    private List<T> resolveAll() {
        return List.copyOf(registry.getAll(pointType));
    }

    /**
     * 失效缓存，下次访问时重新从注册中心解析。
     */
    private void invalidateCache() {
        cachedAll = null;
        cachedTop = null;
    }
}
