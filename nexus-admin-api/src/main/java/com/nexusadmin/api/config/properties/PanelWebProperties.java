package com.nexusadmin.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理面板 Web 层配置属性。
 * <p>
 * 管理面板 API 的基础路径和公开端点列表，供过滤器、拦截器等横切关注点组件使用。
 * 所有路径均支持在 application.yml 中通过 {@code panel.web} 前缀自定义。
 * <p>
 * 过滤器/拦截器仅感知基路径（basePath），不感知具体 API 版本号，
 * 以确保新增版本时无需修改横切关注点配置，符合开闭原则。
 */
@Component
@ConfigurationProperties(prefix = "panel.web")
public class PanelWebProperties {

    /**
     * 管理面板 API 基础路径，不含版本号。
     * <p>过滤器/拦截器基于此值决定拦截范围，默认 "/admin"。
     */
    private String basePath = "/admin";

    /**
     * 公开端点路径列表，无需认证即可访问。
     * <p>支持 Ant 风格路径模式（如 {@code /webjars/**}）。
     * 默认包含登录端点、API 文档端点及静态资源路径。
     */
    private List<String> publicEndpoints = new ArrayList<>();

    /**
     * 获取管理面板 API 基础路径。
     *
     * @return 基础路径，默认 "/admin"
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * 设置管理面板 API 基础路径。
     *
     * @param basePath 基础路径
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * 获取公开端点路径列表。
     *
     * @return 公开端点路径列表
     */
    public List<String> getPublicEndpoints() {
        return publicEndpoints;
    }

    /**
     * 设置公开端点路径列表。
     *
     * @param publicEndpoints 公开端点路径列表
     */
    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }
}
