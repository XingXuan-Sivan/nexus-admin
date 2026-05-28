package com.nexusadmin.api.config;

import com.nexusadmin.api.web.PluginWebRegistry;
import com.nexusadmin.api.util.HttpAuthUtils;
import com.nexusadmin.core.PluginManager;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.config.ConfigManager;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Knife4j / OpenAPI 配置类。
 *
 * <p>配置 OpenAPI 基础信息、SecurityScheme（Bearer Token 认证），
 * 并提供 GlobalOpenApiCustomizer 用于为插件端点标记归属信息。
 * 平台信息从配置中心动态获取，不硬编码。
 * SpringDoc 自动解析 @Operation、@ApiResponse 等标准 OpenAPI 注解，
 * 本配置类仅补充平台级元数据（x-plugin-id）。</p>
 */
@Configuration
public class OpenApiAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenApiAutoConfig.class);

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
                .addSecurityItem(createSecurityRequirement());
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
            SecurityRequirement requirement = createSecurityRequirement();
            openApi.getPaths().values().forEach(pathItem -> {
                for (Operation op : pathItem.readOperations()) {
                    op.addSecurityItem(requirement);
                }
            });
        };
    }

    /**
     * 插件端点 OpenAPI 定制器。
     *
     * <p>从插件 Web 端点注册表获取各插件已注册到 Spring MVC 的端点，
     * 若 SpringDoc 已自动发现则补充 x-plugin-id 归属标记，
     * 若未发现（插件控制器非 Spring Bean）则主动创建 PathItem 与 Operation，
     * 并读取控制器方法上的 @Operation、@Tag 等标准 OpenAPI 注解填充文档。</p>
     *
     * @param pluginManager  插件管理器
     * @param webRegistry    插件 Web 端点注册表
     * @param handlerMapping Spring MVC 请求映射处理器
     * @return GlobalOpenApiCustomizer 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "pluginOpenApiCustomizer")
    public GlobalOpenApiCustomizer pluginOpenApiCustomizer(
            PluginManager pluginManager,
            PluginWebRegistry webRegistry,
            RequestMappingHandlerMapping handlerMapping) {
        return openApi -> {
            var activePlugins = pluginManager.listByState(PluginState.ACTIVE);
            var handlerMethods = handlerMapping.getHandlerMethods();
            SecurityRequirement security = createSecurityRequirement();

            log.debug("插件 OpenAPI 定制器开始执行 (活跃插件数: {}, HandlerMethod 总数: {}, Paths 当前数量: {})",
                    activePlugins.size(), handlerMethods.size(),
                    openApi.getPaths() != null ? openApi.getPaths().size() : 0);

            if (openApi.getPaths() == null) {
                log.warn("OpenAPI Paths 为 null，跳过插件端点处理");
                return;
            }

            // 清理已下线插件的端点路径
            var activePluginIds = activePlugins.stream()
                    .map(plugin -> plugin.getPluginId())
                    .collect(Collectors.toSet());
            cleanupInactivePluginPaths(openApi, activePluginIds);

            for (var plugin : activePlugins) {
                String pluginId = plugin.getPluginId();
                var registryMappings = webRegistry.get(pluginId);

                log.debug("处理插件端点 (插件: {}, 注册映射数: {})", pluginId, registryMappings.size());

                if (registryMappings.isEmpty()) {
                    log.info("插件 {} 无注册的 Web 端点映射", pluginId);
                    continue;
                }

                for (Object entry : registryMappings) {
                    RequestMappingInfo mappingInfo = (RequestMappingInfo) entry;
                    String path = extractPath(mappingInfo);
                    if (path != null) {
                        log.debug("处理路径: {} (插件: {})", path, pluginId);
                        processPluginPath(openApi, pluginId, path, mappingInfo, handlerMethods, security);
                    } else {
                        log.debug("RequestMappingInfo 无可提取的路径模式 (插件: {}), 跳过", pluginId);
                    }
                }
            }

            log.debug("插件 OpenAPI 定制器执行完毕 (Paths 最终数量: {})", openApi.getPaths().size());
        };
    }

    /**
     * 创建统一的安全要求对象。
     *
     * @return SecurityRequirement 实例
     */
    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement().addList(HttpAuthUtils.AUTH_HEADER);
    }

    /**
     * 从 RequestMappingInfo 中提取路径模式。
     *
     * <p>Spring 6 使用 PathPatternsCondition（解析后路径），
     * Spring 5 使用 PatternsCondition（Ant 风格路径），
     * 按优先级依次尝试提取。</p>
     *
     * @param mappingInfo 请求映射信息
     * @return 路径字符串，无法提取时返回 null
     */
    private String extractPath(RequestMappingInfo mappingInfo) {
        var pathPatternsCondition = mappingInfo.getPathPatternsCondition();
        if (pathPatternsCondition != null) {
            var pathPatterns = pathPatternsCondition.getPatternValues();
            if (!pathPatterns.isEmpty()) {
                return pathPatterns.iterator().next();
            }
        }

        var patternsCondition = mappingInfo.getPatternsCondition();
        if (patternsCondition != null) {
            var patterns = patternsCondition.getPatterns();
            if (!patterns.isEmpty()) {
                return patterns.iterator().next();
            }
        }

        return null;
    }

    /**
     * 清理已下线插件在 OpenAPI 规范中遗留的端点路径。
     *
     * <p>遍历当前 paths 中标记了 x-plugin-id 扩展属性的端点，
     * 若对应插件不在活跃集合中则移除该路径，确保文档与运行时状态一致。</p>
     *
     * @param openApi          OpenAPI 规范对象
     * @param activePluginIds  当前活跃插件的 ID 集合
     */
    private void cleanupInactivePluginPaths(OpenAPI openApi, Set<String> activePluginIds) {
        var pathsToRemove = new ArrayList<String>();
        for (var entry : openApi.getPaths().entrySet()) {
            for (var op : entry.getValue().readOperations()) {
                var ext = op.getExtensions();
                if (ext != null && ext.containsKey("x-plugin-id")) {
                    String ownerId = (String) ext.get("x-plugin-id");
                    if (!activePluginIds.contains(ownerId)) {
                        pathsToRemove.add(entry.getKey());
                        log.info("移除已下线插件的端点路径: {} (插件: {})", entry.getKey(), ownerId);
                    }
                    break;
                }
            }
        }
        for (String path : pathsToRemove) {
            openApi.getPaths().remove(path);
        }
    }

    /**
     * 从 HandlerMethod 构建 OpenAPI Operation。
     *
     * <p>读取方法上的 @Operation 注解获取摘要、描述及响应信息，
     * 读取类上的 @Tag 注解获取分组标签，同时标记 x-plugin-id 归属。</p>
     *
     * @param handlerMethod 处理器方法
     * @param pluginId      插件标识
     * @return OpenAPI Operation 实例
     */
    private Operation buildPluginOperation(HandlerMethod handlerMethod, String pluginId) {
        Operation operation = new Operation();
        Method method = handlerMethod.getMethod();

        // 读取 @Operation 注解
        var opAnnotation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
        if (opAnnotation != null) {
            operation.summary(opAnnotation.summary());
            operation.description(opAnnotation.description());
            var apiResponses = new ApiResponses();
            for (var apiResponse : opAnnotation.responses()) {
                apiResponses.addApiResponse(apiResponse.responseCode(),
                        new ApiResponse().description(apiResponse.description()));
            }
            operation.responses(apiResponses);
        } else {
            operation.summary(method.getName());
        }

        // 读取 @Tag（类级别优先）
        var tagAnnotation = handlerMethod.getBeanType()
                .getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
        if (tagAnnotation != null) {
            operation.addTagsItem(tagAnnotation.name());
        } else {
            operation.addTagsItem(pluginId);
        }

        operation.addExtension("x-plugin-id", pluginId);
        return operation;
    }

    /**
     * 处理单个插件端点路径：补充已有路径的归属标记或为缺失路径创建 PathItem。
     *
     * @param openApi       OpenAPI 规范对象
     * @param pluginId      插件标识
     * @param path          请求路径
     * @param mappingInfo   请求映射信息
     * @param handlerMethods 所有已注册的 HandlerMethod
     * @param security      安全要求
     */
    private void processPluginPath(OpenAPI openApi, String pluginId, String path,
                                    RequestMappingInfo mappingInfo,
                                    Map<RequestMappingInfo, HandlerMethod> handlerMethods,
                                    SecurityRequirement security) {
        HandlerMethod handlerMethod = handlerMethods.get(mappingInfo);

        PathItem existing = openApi.getPaths().get(path);
        if (existing != null) {
            log.debug("SpringDoc 已发现端点 {}, 补充 x-plugin-id={}", path, pluginId);
            for (var op : existing.readOperations()) {
                op.addExtension("x-plugin-id", pluginId);
            }
        } else if (handlerMethod != null) {
            log.debug("SpringDoc 未发现端点 {}, 主动创建 PathItem (HandlerMethod={})",
                    path, handlerMethod.getMethod().getName());
            PathItem newPathItem = new PathItem();
            Operation operation = buildPluginOperation(handlerMethod, pluginId);
            operation.addSecurityItem(security);

            var methods = mappingInfo.getMethodsCondition().getMethods();
            for (var httpMethod : methods) {
                switch (httpMethod) {
                    case GET    -> newPathItem.get(operation);
                    case POST   -> newPathItem.post(operation);
                    case PUT    -> newPathItem.put(operation);
                    case DELETE -> newPathItem.delete(operation);
                    case PATCH  -> newPathItem.patch(operation);
                    default     -> {}
                }
            }
            openApi.getPaths().addPathItem(path, newPathItem);
            log.debug("已创建 PathItem (路径: {}, 方法: {})", path, methods);
        } else {
            log.warn("HandlerMethod 未找到 (路径: {}, pluginId: {}), 无法创建 PathItem",
                    path, pluginId);
        }
    }
}
