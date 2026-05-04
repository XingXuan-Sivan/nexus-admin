package com.nexusadmin.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.extension.permission.PermissionResolver;
import com.nexusadmin.api.interceptor.PermissionInterceptor;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 权限拦截体系装配配置。
 * <p>
 * 装配权限解析器扩展点消费者与权限检查拦截器，为管理面板 API 提供声明式权限校验能力。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; 默认实现
 */
@Configuration
public class PermissionAutoConfig {

    /**
     * 权限解析器扩展点消费者。
     * <p>提供缓存 + 事件驱动失效的动态解析能力，供 PermissionInterceptor 使用。</p>
     * <p>使用 name 限定而非类型限定，以避免与其他 ExtensionConsumer 发生泛型擦除冲突。</p>
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @return ExtensionConsumer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "permissionResolverConsumer")
    public ExtensionConsumer<PermissionResolver> permissionResolverConsumer(
            ExtensionRegistry extensionRegistry,
            EventBus eventBus) {
        return new ExtensionConsumer<>(PermissionResolver.class, extensionRegistry, eventBus);
    }

    /**
     * 权限检查拦截器。
     * <p>基于 {@link com.nexusadmin.api.auth.RequirePermission} 注解对管理面板 API 进行权限校验。</p>
     *
     * @param resolverConsumer 权限解析器扩展点消费者
     * @param objectMapper     JSON 序列化器
     * @return PermissionInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean(PermissionInterceptor.class)
    public PermissionInterceptor permissionInterceptor(
            ExtensionConsumer<PermissionResolver> resolverConsumer,
            ObjectMapper objectMapper) {
        return new PermissionInterceptor(resolverConsumer, objectMapper);
    }
}
