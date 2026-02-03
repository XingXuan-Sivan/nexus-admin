package com.nexusadmin.plugin.registry;

import com.nexusadmin.core.spi.SpiRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认的 SPI 注册中心实现。
 */
public class DefaultSpiRegistry implements SpiRegistry {
    private static final class RegisteredSpi {
        private final Object implementation;
        private final int priority;
        private final long sequence;

        private RegisteredSpi(Object implementation, int priority, long sequence) {
            this.implementation = implementation;
            this.priority = priority;
            this.sequence = sequence;
        }
    }

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<RegisteredSpi>> registry = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> spiType, T implementation) {
        register(spiType, implementation, 0);
    }

    @Override
    public <T> void register(Class<T> spiType, T implementation, int priority) {
        if (spiType == null || implementation == null) {
            return;
        }
        registry
                .computeIfAbsent(spiType, key -> new CopyOnWriteArrayList<>())
                .add(new RegisteredSpi(implementation, priority, SEQUENCE.incrementAndGet()));
    }

    @Override
    public <T> void unregister(Class<T> spiType, T implementation) {
        if (spiType == null || implementation == null) {
            return;
        }
        CopyOnWriteArrayList<RegisteredSpi> list = registry.get(spiType);
        if (list != null) {
            list.removeIf(registered -> registered.implementation == implementation || registered.implementation.equals(implementation));
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> spiType) {
        List<T> list = getAll(spiType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getAll(Class<T> spiType) {
        if (spiType == null) {
            return List.of();
        }
        CopyOnWriteArrayList<RegisteredSpi> list = registry.get(spiType);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(registered -> spiType.isInstance(registered.implementation))
                .sorted(Comparator
                        .comparingInt((RegisteredSpi r) -> r.priority).reversed()
                        .thenComparing((RegisteredSpi r) -> r.sequence, Comparator.reverseOrder()))
                .map(registered -> (T) registered.implementation)
                .toList();
    }
}
