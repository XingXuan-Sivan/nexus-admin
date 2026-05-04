package com.nexusadmin.api.config;

import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.config.ConfigManager;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

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
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入 Bearer Token")));
    }

    /**
     * OpenAPI 定制器，将插件动态注册的 API 纳入文档。
     *
     * <p>遍历当前所有已激活插件，为每个插件的路由贡献自动补充 OpenAPI 路径条目。</p>
     *
     * @param pluginManager 插件管理器
     * @return OpenApiCustomizer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "pluginOpenApiCustomizer")
    public OpenApiCustomizer pluginOpenApiCustomizer(PluginManager pluginManager) {
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
