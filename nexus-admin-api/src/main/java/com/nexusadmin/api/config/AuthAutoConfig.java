package com.nexusadmin.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.auth.*;
import com.nexusadmin.api.auth.impl.BootstrapAuthProvider;
import com.nexusadmin.api.auth.CompositeAuthProvider;
import com.nexusadmin.api.auth.impl.DefaultAuthChallengeHandler;
import com.nexusadmin.api.auth.PermissionInterceptor;
import com.nexusadmin.api.config.properties.PanelWebProperties;
import com.nexusadmin.api.auth.AuthChallengeHandler;
import com.nexusadmin.api.auth.AuthProvider;
import com.nexusadmin.api.auth.PermissionResolver;
import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 认证体系装配配置。
 * <p>
 * 负责装配认证体系相关的扩展点组件，包括引导认证提供者、认证挑战处理器、
 * 扩展点消费者以及认证过滤器。所有组件均通过 {@link ExtensionRegistry}
 * 注册，支持运行时热替换。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; 默认实现
 */
@Configuration
public class AuthAutoConfig {

    /**
     * 引导认证提供者。
     * <p>
     * 提供基于配置中心的管理员认证能力，注册到扩展注册中心作为最低优先级兜底实现。
     *
     * @param configManager      配置管理器，用于动态读取管理员凭据
     * @param extensionRegistry  扩展注册中心
     * @return BootstrapAuthProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(BootstrapAuthProvider.class)
    public BootstrapAuthProvider bootstrapAuthProvider(ConfigManager configManager,
                                                       ExtensionRegistry extensionRegistry,
                                                       @Value("${panel.auth.bootstrap-password:}")
                                                       String bootstrapPassword) {
        BootstrapAuthProvider provider = new BootstrapAuthProvider(configManager, bootstrapPassword);
        extensionRegistry.register(AuthProvider.class, provider, 25);
        return provider;
    }

    /**
     * 默认认证挑战处理器。
     * <p>
     * 返回标准 HTTP 401 响应，注册到扩展注册中心作为最低优先级兜底实现。
     *
     * @param extensionRegistry 扩展注册中心
     * @return DefaultAuthChallengeHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean(DefaultAuthChallengeHandler.class)
    public DefaultAuthChallengeHandler defaultAuthChallengeHandler(ExtensionRegistry extensionRegistry) {
        DefaultAuthChallengeHandler handler = new DefaultAuthChallengeHandler();
        extensionRegistry.register(AuthChallengeHandler.class, handler, 10);
        return handler;
    }

    /**
     * 认证提供者扩展点消费者。
     * <p>
     * 提供缓存 + 事件驱动失效的动态解析能力，供 CompositeAuthProvider 使用。
     * 使用 name 限定而非类型限定，以避免与其他 ExtensionConsumer 发生泛型擦除冲突。
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @return ExtensionConsumer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "authProviderConsumer")
    public ExtensionConsumer<AuthProvider> authProviderConsumer(ExtensionRegistry extensionRegistry,
                                                                 EventBus eventBus) {
        return new ExtensionConsumer<>(AuthProvider.class, extensionRegistry, eventBus);
    }

    /**
     * 认证挑战处理器扩展点消费者。
     * <p>
     * 提供缓存 + 事件驱动失效的动态解析能力，供 AuthFilter 使用。
     * 使用 name 限定而非类型限定，以避免与其他 ExtensionConsumer 发生泛型擦除冲突。
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @return ExtensionConsumer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "challengeHandlerConsumer")
    public ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer(ExtensionRegistry extensionRegistry,
                                                                             EventBus eventBus) {
        return new ExtensionConsumer<>(AuthChallengeHandler.class, extensionRegistry, eventBus);
    }

    /**
     * 组合认证提供者。
     * <p>
     * 通过 ExtensionConsumer 动态获取所有注册的 AuthProvider，按优先级依次尝试认证。
     * 当存在非引导认证提供者时，自动排除引导认证。
     *
     * @param authProviderConsumer 认证提供者扩展点消费者
     * @return CompositeAuthProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(CompositeAuthProvider.class)
    public CompositeAuthProvider compositeAuthProvider(ExtensionConsumer<AuthProvider> authProviderConsumer) {
        return new CompositeAuthProvider(authProviderConsumer);
    }

    /**
     * 管理面板认证过滤器。
     * <p>
     * 拦截管理面板基路径（默认 /admin/*）下的所有请求，支持 Bearer Token、
     * Basic 认证和 Session 认证。基路径不含版本号，新增 API 版本时无需修改
     * 过滤器配置。动态从扩展注册中心解析认证挑战处理器。
     *
     * @param properties                管理面板 Web 配置属性
     * @param authProvider              组合认证提供者
     * @param challengeHandlerConsumer  认证挑战处理器扩展点消费者
     * @return 过滤器注册 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "adminAuthFilter")
    public FilterRegistrationBean<AuthFilter> adminAuthFilter(
            PanelWebProperties properties,
            CompositeAuthProvider authProvider,
            ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthFilter(properties, authProvider, challengeHandlerConsumer));
        registration.addUrlPatterns(properties.getBasePath() + "/*");
        registration.setName("adminAuthFilter");
        registration.setOrder(1);
        return registration;
    }

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
     * 当前请求权限判定入口，供接口拦截器与业务能力元数据共同使用。
     *
     * @param permissionAccess 当前请求权限判定入口
     * @return 权限判定入口
     */
    @Bean
    @ConditionalOnMissingBean(PermissionAccess.class)
    public PermissionAccess permissionAccess(
            @Qualifier("permissionResolverConsumer")
            ExtensionConsumer<PermissionResolver> resolverConsumer) {
        return new PermissionAccess(resolverConsumer);
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
            PermissionAccess permissionAccess,
            ObjectMapper objectMapper) {
        return new PermissionInterceptor(permissionAccess, objectMapper);
    }
}
