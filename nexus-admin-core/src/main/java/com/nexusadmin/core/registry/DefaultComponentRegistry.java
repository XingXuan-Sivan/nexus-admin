package com.nexusadmin.core.registry;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 默认组件注册中心实现。
 * <p>线程安全，支持优先级排序。</p>
 * <p>实现特点：</p>
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap} 和 {@link CopyOnWriteArrayList} 保证线程安全</li>
 *   <li>优先级数值越大，优先级越高</li>
 *   <li>相同优先级时，后注册的实现优先（基于序列号）</li>
 * </ul>
 */
public class DefaultComponentRegistry implements ComponentRegistry<Composable> {

    /**
     * 已注册组件的内部封装，包含实现实例、优先级和序列号。
     */
    private static final class RegisteredComponent {
        final Object implementation;
        final int priority;
        final long sequence;

        RegisteredComponent(Object implementation, int priority, long sequence) {
            this.implementation = implementation;
            this.priority = priority;
            this.sequence = sequence;
        }
    }

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    /**
     * 组件类型 -> 实现列表 的映射。
     */
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<RegisteredComponent>> registry =
            new ConcurrentHashMap<>();

    @Override
    public <C extends Composable> void register(Class<C> componentType, C implementation) {
        register(componentType, implementation, 0);
    }

    @Override
    public <C extends Composable> void register(Class<C> componentType, C implementation, int priority) {
        Objects.requireNonNull(componentType, "componentType must not be null");
        Objects.requireNonNull(implementation, "implementation must not be null");

        registry
                .computeIfAbsent(componentType, k -> new CopyOnWriteArrayList<>())
                .add(new RegisteredComponent(implementation, priority, SEQUENCE.incrementAndGet()));
    }

    @Override
    public <C extends Composable> void unregister(Class<C> componentType, C implementation) {
        if (componentType == null || implementation == null) {
            return;
        }
        CopyOnWriteArrayList<RegisteredComponent> list = registry.get(componentType);
        if (list != null) {
            list.removeIf(r -> r.implementation == implementation ||
                    r.implementation.equals(implementation));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Composable> Optional<C> get(Class<C> componentType) {
        List<C> list = getAll(componentType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Composable> List<C> getAll(Class<C> componentType) {
        if (componentType == null) {
            return List.of();
        }
        CopyOnWriteArrayList<RegisteredComponent> list = registry.get(componentType);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(r -> componentType.isInstance(r.implementation))
                .sorted(Comparator
                        .comparingInt((RegisteredComponent r) -> r.priority).reversed()
                        .thenComparing((RegisteredComponent r) -> r.sequence, Comparator.reverseOrder()))
                .map(r -> (C) r.implementation)
                .collect(Collectors.toList());
    }

    @Override
    public <C extends Composable> int count(Class<C> componentType) {
        return getAll(componentType).size();
    }

    @Override
    public <C extends Composable> boolean contains(Class<C> componentType, C implementation) {
        if (componentType == null || implementation == null) {
            return false;
        }
        CopyOnWriteArrayList<RegisteredComponent> list = registry.get(componentType);
        if (list == null) {
            return false;
        }
        return list.stream().anyMatch(r -> r.implementation == implementation ||
                r.implementation.equals(implementation));
    }

    @Override
    public <C extends Composable> void clear(Class<C> componentType) {
        if (componentType != null) {
            registry.remove(componentType);
        }
    }
}
