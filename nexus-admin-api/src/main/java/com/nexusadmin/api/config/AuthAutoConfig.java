package com.nexusadmin.api.config;

import com.nexusadmin.api.extension.auth.AuthChallengeHandler;
import com.nexusadmin.api.auth.AuthFilter;
import com.nexusadmin.api.auth.BootstrapAuthChallengeHandler;
import com.nexusadmin.api.auth.BootstrapAuthProvider;
import com.nexusadmin.api.auth.CompositeAuthProvider;
import com.nexusadmin.api.auth.DefaultAuthChallengeHandler;
import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                                                       ExtensionRegistry extensionRegistry) {
        BootstrapAuthProvider provider = new BootstrapAuthProvider(configManager);
        extensionRegistry.register(AuthProvider.class, provider, 25);
        return provider;
    }

    /**
     * 引导认证挑战处理器。
     * <p>
     * 返回 HTML 登录页面，注册到扩展注册中心作为引导优先级实现。
     *
     * @param extensionRegistry 扩展注册中心
     * @return BootstrapAuthChallengeHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean(BootstrapAuthChallengeHandler.class)
    public BootstrapAuthChallengeHandler bootstrapAuthChallengeHandler(ExtensionRegistry extensionRegistry) {
        BootstrapAuthChallengeHandler handler = new BootstrapAuthChallengeHandler();
        extensionRegistry.register(AuthChallengeHandler.class, handler, 25);
        return handler;
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
     * 拦截所有 /admin/v1/* 请求，支持 Bearer Token、Basic 认证、表单登录和 Session 认证。
     * 动态从扩展注册中心解析认证挑战处理器。
     *
     * @param authProvider             组合认证提供者
     * @param challengeHandlerConsumer 认证挑战处理器扩展点消费者
     * @return 过滤器注册 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "adminAuthFilter")
    public FilterRegistrationBean<AuthFilter> adminAuthFilter(
            CompositeAuthProvider authProvider,
            ExtensionConsumer<AuthChallengeHandler> challengeHandlerConsumer) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthFilter(authProvider, challengeHandlerConsumer));
        registration.addUrlPatterns("/admin/v1/*");
        registration.setName("adminAuthFilter");
        registration.setOrder(1);
        return registration;
    }
}
