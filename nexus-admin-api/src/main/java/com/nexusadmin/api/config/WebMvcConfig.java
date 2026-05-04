package com.nexusadmin.api.config;

import com.nexusadmin.api.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置，注册拦截器等 Web 层基础设施。
 *
 * <p>当前注册的拦截器：</p>
 * <ul>
 *   <li>PermissionInterceptor — 权限检查拦截器，拦截 /admin/v1/** 路径</li>
 * </ul>
 *
 * <p>注意：权限相关的 Bean 定义已迁移至 {@link ApiAutoConfig}，
 * 以避免 Spring 容器循环依赖。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    /**
     * 构造 Web MVC 配置。
     *
     * @param permissionInterceptor 权限检查拦截器
     */
    public WebMvcConfig(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/admin/v1/**")
                .order(2);
    }
}
