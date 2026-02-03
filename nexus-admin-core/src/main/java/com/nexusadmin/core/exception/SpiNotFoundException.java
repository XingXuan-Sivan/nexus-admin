package com.nexusadmin.core.exception;

/**
 * SPI 未找到异常，当从注册中心获取指定 SPI 实现但未找到时抛出。
 */
public class SpiNotFoundException extends CoreException {
    /**
     * 构造 SPI 未找到异常。
     *
     * @param spiType 未找到的 SPI 接口类型
     */
    public SpiNotFoundException(Class<?> spiType) {
        super("SPI not found: " + (spiType == null ? "unknown" : spiType.getName()));
    }
}
