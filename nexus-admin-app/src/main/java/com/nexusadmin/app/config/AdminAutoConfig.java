package com.nexusadmin.app.config;

import com.nexusadmin.api.extension.auth.AuthProvider;
import com.nexusadmin.app.auth.AdminAuthFilter;
import com.nexusadmin.app.auth.BootstrapAuthProvider;
import com.nexusadmin.app.auth.CompositeAuthProvider;
import com.nexusadmin.core.config.ConfigManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 管理面板自动配置。
 * <p>
 * 该配置类负责装配管理面板认证相关组件：
 * <ul>
 *   <li>BootstrapAuthProvider - 引导认证提供者</li>
 *   <li>CompositeAuthProvider - 组合认证提供者</li>
 *   <li>AdminAuthFilter - 管理面板认证过滤器</li>
 * </ul>
 * <p>
 * <strong>设计说明：</strong>
 * <ul>
 *   <li>认证组件在 Root Context 中配置，拦截所有 /admin/** 请求</li>
 *   <li>CompositeAuthProvider 自动收集所有 AuthProvider Bean</li>
 *   <li>插件可通过扩展注册中心注册额外的 AuthProvider</li>
 * </ul>
 */
@Configuration
public class AdminAutoConfig {

    /**
     * 引导认证提供者。
     * <p>
     * 提供基于配置文件的管理员认证能力。
     *
     * @param configManager 配置管理器
     * @return BootstrapAuthProvider 实例
     */
    @Bean
    public BootstrapAuthProvider bootstrapAuthProvider(ConfigManager configManager) {
        return new BootstrapAuthProvider(configManager);
    }

    /**
     * 组合认证提供者。
     * <p>
     * 按顺序委托给所有注册的 AuthProvider 进行认证。
     *
     * @param providers 所有可用的 AuthProvider Bean 列表
     * @return CompositeAuthProvider 实例
     */
    @Bean
    public CompositeAuthProvider compositeAuthProvider(List<AuthProvider> providers) {
        return new CompositeAuthProvider(providers);
    }

    /**
     * 管理面板认证过滤器。
     * <p>
     * 拦截所有 /admin/** 请求，要求 Basic 认证。
     *
     * @param authProvider 组合认证提供者
     * @return 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(CompositeAuthProvider authProvider) {
        FilterRegistrationBean<AdminAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminAuthFilter(authProvider));
        registration.addUrlPatterns("/admin/*");
        registration.setName("adminAuthFilter");
        registration.setOrder(1);
        return registration;
    }
}
