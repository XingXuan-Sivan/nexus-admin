package com.nexusadmin.api.extension.web;

import java.util.List;

/**
 * Web 控制器提供者接口。
 * <p>
 * 插件通过实现此接口，向平台提供需要注册的 Web 控制器。
 * 该接口不依赖任何具体 Web 框架。
 * <p>
 * <strong>使用示例：</strong>
 * <pre>
 * public class MyPlugin extends AbstractPlugin implements WebControllerProvider {
 *     &#064;Override
 *     public List&lt;Object&gt; getControllers() {
 *         return List.of(new MyController());
 *     }
 * }
 * </pre>
 */
public interface WebControllerProvider {

    /**
     * 获取该插件提供的所有 Web 控制器实例。
     * <p>
     * 返回的控制器对象通常包含 Web 框架相关的注解（如 Spring MVC 的 @RestController）。
     * 平台会根据当前使用的 Web 框架，将这些控制器注册到对应的请求映射系统中。
     *
     * @return 控制器实例列表，如果没有则返回空列表
     */
    List<Object> getControllers();
}
