package com.nexusadmin.api.ai;

import java.lang.annotation.*;

/**
 * 启用 AI 工具自动扫描的标记注解。
 *
 * <p>插件主类标记该注解后，平台会在插件激活时扫描插件包下的
 * AiTool 实现类与 LangChain4j @Tool 注解方法，
 * 并将它们自动注册到 {@link AiToolRegistry}。</p>
 *
 * <p>与 {@code @EnableWebEndpoints} 对等，二者共用同一套插件类加载器桥接机制，
 * 确保插件的 AiTool 和 @Tool 方法能被主 Spring 容器感知。</p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>
 * &#064;EnableAiTools
 * &#064;EnableWebEndpoints
 * public class MyPlugin extends AbstractPlugin {
 * }
 * </pre>
 *
 * @see AiTool
 * @see AiToolRegistry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableAiTools {

    /**
     * 要扫描的基础包列表。
     * <p>未指定时，默认使用插件主类所在包及其子包。</p>
     *
     * @return 基础包路径数组
     */
    String[] basePackages() default {};

    /**
     * 通过类推导基础包。
     *
     * @return 用于推导基础包的类数组
     */
    Class<?>[] basePackageClasses() default {};
}
