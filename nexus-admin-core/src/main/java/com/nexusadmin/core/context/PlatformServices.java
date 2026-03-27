package com.nexusadmin.core.context;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 平台服务注册中心，提供平台级服务的注册与查询能力。
 * <p>
 * 该类作为平台与外部服务之间的桥梁，允许宿主应用注册服务供插件使用。
 * 核心模块不感知具体服务类型，保持纯粹的插件运行时职责。
 * </p>
 * <p>
 * 典型使用场景：
 * </p>
 * <ul>
 *   <li>宿主应用注册 AdminFacade、AuthFacade、AuditFacade 等管理服务</li>
 *   <li>插件通过类型安全的方式获取所需服务</li>
 * </ul>
 */
public final class PlatformServices {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    /**
     * 注册服务实例。
     *
     * @param <T>      服务类型
     * @param type     服务接口类型
     * @param instance 服务实例
     * @throws IllegalArgumentException 如果类型或实例为空
     */
    public <T> void register(Class<T> type, T instance) {
        if (type == null) {
            throw new IllegalArgumentException("服务类型不能为空");
        }
        if (instance == null) {
            throw new IllegalArgumentException("服务实例不能为空");
        }
        services.put(type, instance);
    }

    /**
     * 获取指定类型的服务实例。
     *
     * @param <T>  服务类型
     * @param type 服务接口类型
     * @return 服务实例的 Optional，如果未注册则返回空
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }
        Object instance = services.get(type);
        if (instance != null && type.isInstance(instance)) {
            return Optional.of((T) instance);
        }
        return Optional.empty();
    }

    /**
     * 检查指定类型的服务是否已注册。
     *
     * @param type 服务接口类型
     * @return 如果已注册返回 true
     */
    public boolean contains(Class<?> type) {
        return type != null && services.containsKey(type);
    }

    /**
     * 注销指定类型的服务。
     *
     * @param type 服务接口类型
     */
    public void unregister(Class<?> type) {
        if (type != null) {
            services.remove(type);
        }
    }

    /**
     * 获取已注册服务的数量。
     *
     * @return 服务数量
     */
    public int size() {
        return services.size();
    }
}
