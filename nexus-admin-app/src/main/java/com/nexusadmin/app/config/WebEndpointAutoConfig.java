package com.nexusadmin.app.config;

import com.nexusadmin.api.extension.web.MappingResolver;
import com.nexusadmin.api.extension.web.PluginWebRegistry;
import com.nexusadmin.api.extension.web.WebEndpointExtension;
import com.nexusadmin.api.extension.web.WebEndpointRegistrar;
import com.nexusadmin.app.web.InMemoryPluginWebRegistry;
import com.nexusadmin.app.web.SpringMappingResolver;
import com.nexusadmin.app.web.SpringWebEndpointExtension;
import com.nexusadmin.app.web.SpringWebEndpointRegistrar;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Web 端点自动配置。
 * <p>
 * 该配置类负责装配插件 Web 接入机制的核心组件：
 * <ul>
 *   <li>PluginWebRegistry - 端点注册表</li>
 *   <li>MappingResolver - 映射解析器</li>
 *   <li>WebEndpointRegistrar - 端点注册器</li>
 *   <li>WebEndpointExtension - Web 接入扩展点</li>
 * </ul>
 */
@Configuration
public class WebEndpointAutoConfig {

    /**
     * 插件 Web 端点注册表。
     *
     * @return InMemoryPluginWebRegistry 实例
     */
    @Bean
    public PluginWebRegistry pluginWebRegistry() {
        return new InMemoryPluginWebRegistry();
    }

    /**
     * 映射解析器。
     *
     * @return SpringMappingResolver 实例
     */
    @Bean
    public MappingResolver mappingResolver() {
        return new SpringMappingResolver();
    }

    /**
     * Web 端点注册器。
     *
     * @param handlerMapping Spring MVC 的 RequestMappingHandlerMapping
     * @param resolver       映射解析器
     * @param registry       插件 Web 端点注册表
     * @return SpringWebEndpointRegistrar 实例
     */
    @Bean
    public WebEndpointRegistrar webEndpointRegistrar(
            RequestMappingHandlerMapping handlerMapping,
            MappingResolver resolver,
            PluginWebRegistry registry) {
        return new SpringWebEndpointRegistrar(handlerMapping, resolver, registry);
    }

    /**
     * Web 接入扩展点。
     * <p>
     * 同时注册到 Spring 容器和 ExtensionRegistry。
     *
     * @param applicationContext Spring 应用上下文
     * @param extensionRegistry  扩展注册中心
     * @return SpringWebEndpointExtension 实例
     */
    @Bean
    public WebEndpointExtension webEndpointExtension(
            ApplicationContext applicationContext,
            ExtensionRegistry extensionRegistry) {
        SpringWebEndpointExtension extension = new SpringWebEndpointExtension(applicationContext);
        extensionRegistry.register(WebEndpointExtension.class, extension, 50);
        return extension;
    }
}
