package com.nexusadmin.api.annotation;

import java.lang.annotation.*;

/**
 * 标记一个类为 SPI 实现，支持 Spring Boot 自动注册。
 * <p>仅适用于 api 模块中定义的 SPI 接口。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;SpiImplementation(value = StorageProvider.class, priority = 100)
 * public class S3StorageProvider implements StorageProvider {
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpiImplementation {

    /**
     * SPI 接口类型。
     * <p>默认为 Void.class，表示自动推断当前类实现的第一个接口。</p>
     *
     * @return SPI 接口类型
     */
    Class<?> value() default Void.class;

    /**
     * 注册优先级，数值越大优先级越高。
     * <p>当多个实现优先级相同时，后注册的实现优先。</p>
     *
     * @return 优先级数值
     */
    int priority() default 0;
}
