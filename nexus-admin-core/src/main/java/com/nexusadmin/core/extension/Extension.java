package com.nexusadmin.core.extension;

import java.lang.annotation.*;

/**
 * 扩展实现标记注解。
 * <p>用于标记扩展点实现类，支持自动发现、优先级配置、扩展点推断。</p>
 * <p>被标记的类会在编译期被扫描并生成索引，运行期通过 {@link ExtensionPoint} 体系自动发现。</p>
 *
 * @see ExtensionPoint
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Extension {

    /**
     * 显式声明实现了哪些扩展点接口。
     * <p>如果为空数组，则自动从该类实现的接口中推断出所有 {@link ExtensionPoint} 子接口。</p>
     *
     * @return 扩展点接口类型数组
     */
    Class<? extends ExtensionPoint>[] points() default {};

    /**
     * 优先级，值越大优先级越高。
     * <p>当多个实现同时存在时，优先级高的实现会被优先使用。</p>
     *
     * @return 优先级数值，默认为 0
     */
    int priority() default 0;

    /**
     * 是否启用该扩展实现。
     * <p>设置为 false 时，该实现会被忽略，不会被注册到扩展中心。</p>
     *
     * @return 是否启用，默认为 true
     */
    boolean enabled() default true;

    /**
     * 扩展实现的逻辑名称。
     * <p>用于后台 UI 展示、日志记录等场景。</p>
     *
     * @return 扩展名称，默认为空字符串
     */
    String name() default "";

    /**
     * 扩展实现的描述信息。
     * <p>用于说明该扩展的功能、用途等。</p>
     *
     * @return 扩展描述，默认为空字符串
     */
    String description() default "";
}
