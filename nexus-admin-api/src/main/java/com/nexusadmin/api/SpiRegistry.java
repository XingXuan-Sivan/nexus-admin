package com.nexusadmin.api;

import java.util.List;
import java.util.Optional;

/**
 * SPI 注册中心接口，负责注册、注销以及查询各类平台能力的实现。
 */
public interface SpiRegistry {
    /**
     * 注册一个 SPI 实现，使用默认优先级 0。
     *
     * @param spiType        SPI 接口类型
     * @param implementation 具体实现实例
     * @param <T>            SPI 接口泛型
     */
    <T> void register(Class<T> spiType, T implementation);
    
    /**
     * 按指定优先级注册一个 SPI 实现。
     * <p>优先级数值越大，优先级越高；当优先级相同时，后注册的实现优先。</p>
     *
     * @param spiType        SPI 接口类型
     * @param implementation 具体实现实例
     * @param priority       优先级，数值越大优先级越高
     * @param <T>            SPI 接口泛型
     */
    default <T> void register(Class<T> spiType, T implementation, int priority) {
        register(spiType, implementation);
    }
    
    /**
     * 从注册中心移除一个 SPI 实现。
     *
     * @param spiType        SPI 接口类型
     * @param implementation 具体实现实例
     * @param <T>            SPI 接口泛型
     */
    <T> void unregister(Class<T> spiType, T implementation);

    /**
     * 获取某个 SPI 类型的单个实现，一般为优先级最高的实现。
     * 当存在多个相同优先级的实现时，返回最后注册的实现。
     *
     * @param spiType SPI 接口类型
     * @param <T>     SPI 接口泛型
     * @return SPI 实现，可为空
     */
    <T> Optional<T> get(Class<T> spiType);

    /**
     * 获取某个 SPI 类型的全部实现。
     *
     * @param spiType SPI 接口类型
     * @param <T>     SPI 接口泛型
     * @return 所有实现组成的不可变列表
     */
    <T> List<T> getAll(Class<T> spiType);
}
