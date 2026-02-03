package com.nexusadmin.infra.logging.config;

import com.nexusadmin.infra.logging.interceptor.HttpAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 日志相关 Web 配置，将 HTTP 访问拦截器注册到 Spring MVC 拦截链。
 */
@Configuration
public class LoggingInfraConfiguration implements WebMvcConfigurer {
    private final HttpAccessInterceptor httpAccessInterceptor;

    /**
     * 构造配置类。
     *
     * @param httpAccessInterceptor HTTP 访问日志拦截器
     */
    public LoggingInfraConfiguration(HttpAccessInterceptor httpAccessInterceptor) {
        this.httpAccessInterceptor = httpAccessInterceptor;
    }

    /**
     * 将 HTTP 访问日志拦截器加入 Spring MVC 拦截链，对所有路径生效。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpAccessInterceptor).addPathPatterns("/**");
    }
}
