package com.nexusadmin.core.facade;

import com.nexusadmin.core.extension.ExtensionPoint;
import com.nexusadmin.core.extension.ExtensionRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 扩展注册中心门面。
 * <p>聚合 {@link ExtensionRegistry}，提供统一的扩展点注册、查询与注销入口。</p>
 */
public class ExtensionFacade {

    private final ExtensionRegistry extensionRegistry;

    /**
     * 构造扩展注册中心门面。
     *
     * @param extensionRegistry 扩展注册中心实例，不能为空
     */
    public ExtensionFacade(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = Objects.requireNonNull(extensionRegistry, "扩展注册中心不能为空");
    }

    /**
     * 注册一个扩展实现，使用默认优先级 0。
     *
     * @param pointType      扩展点接口类型
     * @param implementation 具体实现实例
     * @param <T>            扩展点接口泛型
     */
    public <T extends ExtensionPoint> void register(Class<T> pointType, T implementation) {
        extensionRegistry.register(pointType, implementation);
    }

    /**
     * 按指定优先级注册一个扩展实现。
     *
     * @param pointType      扩展点接口类型
     * @param implementation 具体实现实例
     * @param priority       优先级，数值越大优先级越高
     * @param <T>            扩展点接口泛型
     */
    public <T extends ExtensionPoint> void register(Class<T> pointType, T implementation, int priority) {
        extensionRegistry.register(pointType, implementation, priority);
    }

    /**
     * 从注册中心移除一个扩展实现。
     *
     * @param pointType      扩展点接口类型
     * @param implementation 具体实现实例
     * @param <T>            扩展点接口泛型
     */
    public <T extends ExtensionPoint> void unregister(Class<T> pointType, T implementation) {
        extensionRegistry.unregister(pointType, implementation);
    }

    /**
     * 获取某个扩展点类型的单个实现（一般为优先级最高的实现）。
     *
     * @param pointType 扩展点接口类型
     * @param <T>       扩展点接口泛型
     * @return 扩展实现，可为空
     */
    public <T extends ExtensionPoint> Optional<T> getFirst(Class<T> pointType) {
        return extensionRegistry.get(pointType);
    }

    /**
     * 获取某个扩展点类型的全部实现。
     *
     * @param pointType 扩展点接口类型
     * @param <T>       扩展点接口泛型
     * @return 所有实现组成的列表
     */
    public <T extends ExtensionPoint> List<T> getExtensions(Class<T> pointType) {
        return extensionRegistry.getAll(pointType);
    }

    /**
     * 根据插件ID注销该插件注册的所有扩展实现。
     *
     * @param pluginId 插件ID
     */
    public void unregisterPlugin(String pluginId) {
        extensionRegistry.unregisterByPluginId(pluginId);
    }

    /**
     * 清空指定扩展点类型的所有注册实现。
     *
     * @param pointType 扩展点接口类型
     * @param <T>       扩展点接口泛型
     */
    public <T extends ExtensionPoint> void clear(Class<T> pointType) {
        extensionRegistry.clear(pointType);
    }

    /**
     * 清空所有扩展点注册信息。
     */
    public void clearAll() {
        extensionRegistry.clearAll();
    }

    /**
     * 获取底层扩展注册中心实例。
     *
     * @return 扩展注册中心
     */
    public ExtensionRegistry extensionRegistry() {
        return extensionRegistry;
    }
}
