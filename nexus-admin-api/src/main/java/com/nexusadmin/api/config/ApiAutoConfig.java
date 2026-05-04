package com.nexusadmin.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.controller.PluginStaticResourceController;
import com.nexusadmin.api.extension.permission.PermissionResolver;
import com.nexusadmin.api.extension.ui.UIContributionRegistry;
import com.nexusadmin.api.extension.ui.UIContributionRegistryImpl;
import com.nexusadmin.api.interceptor.PermissionInterceptor;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import com.nexusadmin.core.extension.ExtensionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 基础设施自动配置。
 * <p>
 * 统一管理 API 模块的基础设施组件注册，包括 UI 贡献注册表、权限拦截器、
 * 插件静态资源控制器等。
 * <p>
 * 所有 Bean 均带 {@link ConditionalOnMissingBean} 保护，应用层可通过声明同类型 Bean
 * 覆盖任意组件。
 * <p>
 * <strong>覆盖优先级：</strong>app @Bean &gt; api @ConditionalOnMissingBean &gt; 默认实现
 */
@Configuration
public class ApiAutoConfig {

    // ==================== UI 贡献注册表 ====================

    /**
     * UI 贡献注册表实现。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param pluginManager 插件管理器
     * @return UIContributionRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(UIContributionRegistry.class)
    public UIContributionRegistry uiContributionRegistry(PluginManager pluginManager) {
        return new UIContributionRegistryImpl(pluginManager);
    }

    // ==================== 权限拦截器 ====================

    /**
     * 权限解析器扩展点消费者。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @return ExtensionConsumer 实例
     */
    @Bean
    @ConditionalOnMissingBean(ExtensionConsumer.class)
    public ExtensionConsumer<PermissionResolver> permissionResolverConsumer(
            ExtensionRegistry extensionRegistry,
            EventBus eventBus) {
        return new ExtensionConsumer<>(PermissionResolver.class, extensionRegistry, eventBus);
    }

    /**
     * 权限检查拦截器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
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

    // ==================== 插件静态资源控制器 ====================

    /**
     * 插件静态资源托管控制器。
     * <p>可通过声明同类型 Bean 覆盖此默认装配</p>
     *
     * @param pluginManager 插件管理器
     * @return PluginStaticResourceController 实例
     */
    @Bean
    @ConditionalOnMissingBean(PluginStaticResourceController.class)
    public PluginStaticResourceController pluginStaticResourceController(PluginManager pluginManager) {
        return new PluginStaticResourceController(pluginManager);
    }
}
