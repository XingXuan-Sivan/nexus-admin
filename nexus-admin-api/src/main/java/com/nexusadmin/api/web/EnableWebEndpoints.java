package com.nexusadmin.api.web;

import java.lang.annotation.*;

/**
 * 启用插件 Web 端点自动扫描的标记注解。
 * <p>
 * 插件主类标记该注解后，平台会在插件激活时，
 * 根据配置的包路径为插件扫描 Web 端点。
 * </p>
 * <p><strong>使用示例：</strong></p>
 * <pre>
 * &#064;EnableWebEndpoints
 * public class MyPlugin extends AbstractPlugin {
 * }
 * </pre>
 * <p>或指定扫描包：</p>
 * <pre>
 * &#064;EnableWebEndpoints(basePackages = "com.example.plugin.web")
 * public class MyPlugin extends AbstractPlugin {
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableWebEndpoints {

    /**
     * 要扫描的基础包列表。
     * <p>未指定时，默认使用插件主类所在包及其子包。</p>
     *
     * @return 基础包路径数组
     */
    String[] basePackages() default {};

    /**
     * 通过类推导基础包。
     * <p>未指定时可忽略。</p>
     *
     * @return 用于推导基础包的类数组
     */
    Class<?>[] basePackageClasses() default {};
}
