package com.nexusadmin.api.config;

import com.nexusadmin.api.auth.*;
import com.nexusadmin.api.auth.AuthFilter;
import com.nexusadmin.api.config.properties.BootstrapAuthProperties;
import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.api.management.AdminFacade;
import com.nexusadmin.api.management.impl.AdminFacadeImpl;
import com.nexusadmin.core.DefaultPluginManager;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.config.ConfigManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 管理面板自动配置。
 * <p>
 * 该配置类负责装配管理面板相关组件：
 * <ul>
 *   <li>BootstrapAuthProvider - 引导认证提供者</li>
 *   <li>CompositeAuthProvider - 组合认证提供者（自动排除引导认证）</li>
 *   <li>AuthChallengeHandler - 认证挑战处理器（引导登录页或默认 401 响应）</li>
 *   <li>AuthFilter - 管理面板认证过滤器</li>
 *   <li>AdminFacade - 管理门面（注册到平台服务注册中心）</li>
 * </ul>
 * <p>
 * <strong>设计说明：</strong>
 * <ul>
 *   <li>认证组件在 Root Context 中配置，拦截所有 /admin/** 请求</li>
 *   <li>CompositeAuthProvider 自动收集所有 AuthProvider Bean，当存在其他提供者时自动排除引导认证</li>
 *   <li>认证挑战处理器根据引导认证是否活跃来选择：引导认证活跃时使用 HTML 登录页，否则使用 401 JSON 响应</li>
 *   <li>插件可通过扩展注册中心注册额外的 AuthProvider</li>
 * </ul>
 */
@Configuration
@ComponentScan("com.nexusadmin.api")
public class AdminAutoConfig {

    /**
     * 引导认证提供者。
     * <p>
     * 提供基于配置文件的管理员认证能力。
     *
     * @param properties 引导认证配置属性
     * @return BootstrapAuthProvider 实例
     */
    @Bean
    public BootstrapAuthProvider bootstrapAuthProvider(BootstrapAuthProperties properties) {
        return new BootstrapAuthProvider(properties);
    }

    /**
     * 组合认证提供者。
     * <p>
     * 按顺序委托给所有注册的 AuthProvider 进行认证。
     * 当存在非引导认证提供者时，自动排除引导认证。
     *
     * @param providers 所有可用的 AuthProvider Bean 列表
     * @return CompositeAuthProvider 实例
     */
    @Bean
    public CompositeAuthProvider compositeAuthProvider(List<AuthProvider> providers) {
        return new CompositeAuthProvider(providers);
    }

    /**
     * 认证挑战处理器。
     * <p>
     * 根据引导认证是否被排除来选择实现：
     * <ul>
     *   <li>引导认证活跃（仅有 BootstrapAuthProvider）→ 使用 BootstrapAuthChallengeHandler，提供 HTML 登录页面</li>
     *   <li>引导认证已禁用（存在其他提供者）→ 使用 DefaultAuthChallengeHandler，返回 401 JSON 响应</li>
     * </ul>
     *
     * @param compositeAuthProvider 组合认证提供者
     * @return AuthChallengeHandler 实例
     */
    @Bean
    public AuthChallengeHandler authChallengeHandler(CompositeAuthProvider compositeAuthProvider) {
        boolean bootstrapActive = compositeAuthProvider.getProviders().stream()
                .anyMatch(p -> p instanceof BootstrapAuthProvider);

        if (bootstrapActive) {
            return new BootstrapAuthChallengeHandler();
        }
        return new DefaultAuthChallengeHandler();
    }

    /**
     * 管理面板认证过滤器。
     * <p>
     * 拦截所有 /admin/* 请求，支持 Basic 认证、表单登录和 Session 认证。
     *
     * @param authProvider     组合认证提供者
     * @param challengeHandler 认证挑战处理器
     * @return 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<AuthFilter> adminAuthFilter(
            CompositeAuthProvider authProvider,
            AuthChallengeHandler challengeHandler) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthFilter(authProvider, challengeHandler));
        registration.addUrlPatterns("/admin/*");
        registration.setName("adminAuthFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 管理门面。
     * <p>
     * 创建管理门面并注册到平台服务注册中心，供插件使用。
     *
     * @param pluginManager 插件管理器
     * @param configManager 配置管理器
     * @return AdminFacade 实例
     */
    @Bean
    public AdminFacade adminFacade(PluginManager pluginManager,
                                    ConfigManager configManager) {
        AdminFacade facade = new AdminFacadeImpl(pluginManager, configManager);

        // 将 AdminFacade 注册到平台服务注册中心
        if (pluginManager instanceof DefaultPluginManager dpm) {
            dpm.platformServices().register(AdminFacade.class, facade);
        }

        return facade;
    }
}
