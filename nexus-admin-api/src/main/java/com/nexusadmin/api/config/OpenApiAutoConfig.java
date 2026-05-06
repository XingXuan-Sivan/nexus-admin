package com.nexusadmin.api.config;

import com.nexusadmin.api.util.HttpAuthUtils;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.config.ConfigManager;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 配置类。
 *
 * <p>配置 OpenAPI 基础信息、SecurityScheme（Bearer Token 认证），</p>
 * <p>并提供 OpenApiCustomizer 用于将插件动态注册的 API 纳入 OpenAPI 文档。</p>
 *
 * <p>平台信息从配置中心动态获取，不硬编码。</p>
 */
@Configuration
public class OpenApiAutoConfig {

    /**
     * OpenAPI 基础信息。
     *
     * <p>平台名称、版本、描述从配置中心动态读取，未配置时使用默认值。</p>
     *
     * @param configManager 配置管理器
     * @return OpenAPI 实例
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenAPI(ConfigManager configManager) {
        String title = configManager.get("platform", "infoName").orElse("Nexus Admin");
        String version = configManager.get("platform", "infoVersion").orElse("0.1.0-SNAPSHOT");
        String description = configManager.get("platform", "infoDescription").orElse("插件化系统拓展平台");

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description))
                .components(new Components()
                        .addSecuritySchemes(HttpAuthUtils.AUTH_HEADER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入 Bearer Token")))
                .addSecurityItem(new SecurityRequirement().addList(HttpAuthUtils.AUTH_HEADER));
    }

    /**
     * 全局 OpenAPI 定制器，为所有操作显式添加 BearerAuth 安全要求。
     *
     * <p>SpringDoc 自动发现的 Controller 操作不会自动继承 OpenAPI 对象上
     * 的全局 SecurityRequirement，需通过 GlobalOpenApiCustomizer 逐一注入，
     * 确保 Knife4j 对每个 API 调用均携带 Authorization: Bearer xxx 请求头。</p>
     *
     * @return GlobalOpenApiCustomizer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "securityGlobalCustomizer")
    public GlobalOpenApiCustomizer securityGlobalCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            SecurityRequirement requirement = new SecurityRequirement()
                    .addList(HttpAuthUtils.AUTH_HEADER);
            openApi.getPaths().values().forEach(pathItem -> {
                for (Operation op : pathItem.readOperations()) {
                    op.addSecurityItem(requirement);
                }
            });
        };
    }

    /**
     * OpenAPI 定制器，将插件动态注册的 API 纳入文档并标记插件 ID。
     *
     * <p>遍历当前所有已激活插件，为每个插件的路由贡献自动补充 OpenAPI 路径条目。</p>
     *
     * @param pluginManager 插件管理器
     * @return GlobalOpenApiCustomizer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "pluginOpenApiCustomizer")
    public GlobalOpenApiCustomizer pluginOpenApiCustomizer(PluginManager pluginManager) {
        return openApi -> {
            var activePlugins = pluginManager.listByState(PluginState.ACTIVE);
            for (var plugin : activePlugins) {
                var contributes = plugin.descriptor().contributes();
                // 将插件路由贡献的权限信息写入 OpenAPI 扩展字段
                for (var route : contributes.routes()) {
                    if (openApi.getPaths() != null
                            && openApi.getPaths().get(route.path()) != null) {
                        var pathItem = openApi.getPaths().get(route.path());
                        pathItem.addExtension("x-plugin-id", plugin.getPluginId());
                    }
                }
            }
        };
    }
}
