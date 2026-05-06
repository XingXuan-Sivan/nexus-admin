package com.nexusadmin.api.config;

import com.nexusadmin.api.auth.PermissionInterceptor;
import com.nexusadmin.api.config.properties.PanelWebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置，注册拦截器等 Web 层基础设施。
 *
 * <p>当前注册的拦截器：</p>
 * <ul>
 *   <li>PermissionInterceptor — 权限检查拦截器，拦截管理面板基路径（如 /admin/**），
 *   不感知具体版本号</li>
 * </ul>
 */
@Configuration
public class WebMvcAutoConfig implements WebMvcConfigurer {

    private final PanelWebProperties properties;
    private final PermissionInterceptor permissionInterceptor;

    /**
     * 构造 Web MVC 配置。
     *
     * @param properties             管理面板 Web 配置属性
     * @param permissionInterceptor  权限检查拦截器
     */
    public WebMvcAutoConfig(PanelWebProperties properties,
                            PermissionInterceptor permissionInterceptor) {
        this.properties = properties;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns(properties.getBasePath() + "/**")
                .order(2);
    }
}
