package com.nexusadmin.core.registry;

import java.util.List;
import java.util.Optional;

/**
 * 系统核心组件统一注册中心。
 * <p>负责管理所有具备多实现能力的系统核心组件（parser、resolver、reader、loader 等）。</p>
 * <p>不依赖任何外部框架，可在纯 Java 环境中使用。</p>
 *
 * <p>注册中心支持优先级机制：</p>
 * <ul>
 *   <li>优先级数值越大，优先级越高</li>
 *   <li>当优先级相同时，后注册的实现优先</li>
 * </ul>
 *
 * @param <T> 组件类型，必须继承 {@link Composable}
 */
public interface ComponentRegistry<T extends Composable> {

    /**
     * 注册组件实现，使用默认优先级 0。
     *
     * @param componentType  组件类型接口
     * @param implementation 具体实现实例
     * @param <C>            组件类型泛型
     * @throws NullPointerException 如果 componentType 或 implementation 为 null
     */
    <C extends T> void register(Class<C> componentType, C implementation);

    /**
     * 按指定优先级注册组件实现。
     * <p>优先级数值越大，优先级越高；当优先级相同时，后注册的实现优先。</p>
     *
     * @param componentType  组件类型接口
     * @param implementation 具体实现实例
     * @param priority       优先级，数值越大优先级越高
     * @param <C>            组件类型泛型
     * @throws NullPointerException 如果 componentType 或 implementation 为 null
     */
    <C extends T> void register(Class<C> componentType, C implementation, int priority);

    /**
     * 注销组件实现。
     *
     * @param componentType  组件类型接口
     * @param implementation 要注销的实现实例
     * @param <C>            组件类型泛型
     */
    <C extends T> void unregister(Class<C> componentType, C implementation);

    /**
     * 获取单个组件实现（优先级最高的）。
     * <p>当存在多个相同优先级的实现时，返回最后注册的实现。</p>
     *
     * @param componentType 组件类型接口
     * @param <C>           组件类型泛型
     * @return 优先级最高的实现，如果不存在则返回 empty
     */
    <C extends T> Optional<C> get(Class<C> componentType);

    /**
     * 获取某类型的所有实现，按优先级降序排列。
     * <p>返回的列表不可修改。</p>
     *
     * @param componentType 组件类型接口
     * @param <C>           组件类型泛型
     * @return 按优先级排序的实现列表，不会返回 null
     */
    <C extends T> List<C> getAll(Class<C> componentType);

    /**
     * 获取某类型的所有实现数量。
     *
     * @param componentType 组件类型接口
     * @param <C>           组件类型泛型
     * @return 实现数量
     */
    <C extends T> int count(Class<C> componentType);

    /**
     * 检查是否已注册某实现。
     *
     * @param componentType  组件类型接口
     * @param implementation 要检查的实现实例
     * @param <C>            组件类型泛型
     * @return 如果已注册返回 true
     */
    <C extends T> boolean contains(Class<C> componentType, C implementation);

    /**
     * 清空某类型的所有注册。
     *
     * @param componentType 组件类型接口
     * @param <C>           组件类型泛型
     */
    <C extends T> void clear(Class<C> componentType);
}
