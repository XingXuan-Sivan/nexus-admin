package com.nexusadmin.core.extension;

import java.util.List;
import java.util.Optional;

/**
 * 扩展注册中心接口。
 * <p>负责注册、注销以及查询各类扩展点的实现。</p>
 * <p>扩展注册中心是插件化架构的核心组件，支持多实现、优先级排序、插件级生命周期管理。</p>
 *
 * @see ExtensionPoint
 * @see Extension
 */
public interface ExtensionRegistry {

    /**
     * 注册一个扩展实现，使用默认优先级 0。
     *
     * @param pointType    扩展点接口类型
     * @param implementation 具体实现实例
     * @param <T>          扩展点接口泛型
     */
    <T extends ExtensionPoint> void register(Class<T> pointType, T implementation);

    /**
     * 按指定优先级注册一个扩展实现。
     * <p>优先级数值越大，优先级越高；当优先级相同时，后注册的实现优先。</p>
     *
     * @param pointType      扩展点接口类型
     * @param implementation 具体实现实例
     * @param priority       优先级，数值越大优先级越高
     * @param <T>            扩展点接口泛型
     */
    default <T extends ExtensionPoint> void register(Class<T> pointType, T implementation, int priority) {
        register(pointType, implementation);
    }

    /**
     * 从注册中心移除一个扩展实现。
     *
     * @param pointType      扩展点接口类型
     * @param implementation 具体实现实例
     * @param <T>            扩展点接口泛型
     */
    <T extends ExtensionPoint> void unregister(Class<T> pointType, T implementation);

    /**
     * 获取某个扩展点类型的单个实现，一般为优先级最高的实现。
     * <p>当存在多个相同优先级的实现时，返回最后注册的实现。</p>
     *
     * @param pointType 扩展点接口类型
     * @param <T>       扩展点接口泛型
     * @return 扩展实现，可为空
     */
    <T extends ExtensionPoint> Optional<T> get(Class<T> pointType);

    /**
     * 获取某个扩展点类型的全部实现。
     * <p>返回的列表按优先级降序排列，相同优先级时后注册者在前。</p>
     *
     * @param pointType 扩展点接口类型
     * @param <T>       扩展点接口泛型
     * @return 所有实现组成的不可变列表
     */
    <T extends ExtensionPoint> List<T> getAll(Class<T> pointType);

    /**
     * 根据插件ID注销该插件注册的所有扩展实现。
     * <p>在插件卸载时调用，确保该插件的所有扩展被清理，避免内存泄漏。</p>
     *
     * @param pluginId 插件ID
     */
    void unregisterByPluginId(String pluginId);

    /**
     * 清空指定扩展点类型的所有注册实现。
     *
     * @param pointType 扩展点接口类型
     * @param <T>       扩展点接口泛型
     */
    <T extends ExtensionPoint> void clear(Class<T> pointType);

    /**
     * 清空所有扩展点注册信息。
     */
    void clearAll();
}
