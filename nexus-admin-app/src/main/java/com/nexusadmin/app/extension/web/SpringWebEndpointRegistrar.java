package com.nexusadmin.app.extension.web;

import com.nexusadmin.api.extension.web.MappingResolver;
import com.nexusadmin.api.extension.web.PluginWebRegistry;
import com.nexusadmin.api.extension.web.WebEndpointRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Spring MVC Web 端点注册器实现。
 * <p>
 * 负责将插件的控制器动态注册到 Spring MVC 的 RequestMappingHandlerMapping 中，
 * 并在插件卸载时精确清理所有已注册的端点。
 */
public class SpringWebEndpointRegistrar implements WebEndpointRegistrar {

    private static final Logger log = LoggerFactory.getLogger(SpringWebEndpointRegistrar.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final MappingResolver resolver;
    private final PluginWebRegistry registry;

    /**
     * 构造 Spring Web 端点注册器。
     *
     * @param handlerMapping Spring MVC 的 RequestMappingHandlerMapping
     * @param resolver       映射解析器
     * @param registry       插件 Web 端点注册表
     */
    public SpringWebEndpointRegistrar(
            RequestMappingHandlerMapping handlerMapping,
            MappingResolver resolver,
            PluginWebRegistry registry) {
        this.handlerMapping = handlerMapping;
        this.resolver = resolver;
        this.registry = registry;
    }

    @Override
    public void register(String pluginId, Object controller) {
        List<MappingResolver.ResolvedMapping> mappings = resolver.resolve(controller, pluginId);

        int successCount = 0;
        for (MappingResolver.ResolvedMapping mapping : mappings) {
            try {
                RequestMappingInfo mappingInfo = (RequestMappingInfo) mapping.mappingInfo();
                Method method = mapping.method();
                Object handler = mapping.handler();

                if (hasConflict(mappingInfo)) {
                    log.warn("检测到路径冲突，跳过注册: {} (插件: {})",
                            mappingInfo.getPatternsCondition(), pluginId);
                    continue;
                }

                handlerMapping.registerMapping(mappingInfo, handler, method);
                registry.add(pluginId, mappingInfo);
                successCount++;

                log.debug("注册 Web 端点: {} -> {}#{}",
                        mappingInfo.getPatternsCondition(),
                        handler.getClass().getSimpleName(),
                        method.getName());

            } catch (Exception e) {
                log.error("注册 Web 端点失败 (插件: {}): {}", pluginId, e.getMessage(), e);
            }
        }

        if (successCount > 0) {
            log.info("插件 {} 注册了 {} 个 Web 端点", pluginId, successCount);
        }
    }

    @Override
    public void unregister(String pluginId) {
        List<Object> mappings = registry.get(pluginId);

        if (mappings.isEmpty()) {
            log.debug("插件 {} 无已注册的 Web 端点", pluginId);
            return;
        }

        int removedCount = 0;
        for (Object info : mappings) {
            try {
                handlerMapping.unregisterMapping((RequestMappingInfo) info);
                removedCount++;
            } catch (Exception e) {
                log.warn("卸载 Web 端点失败 (插件: {}): {}", pluginId, e.getMessage());
            }
        }

        registry.remove(pluginId);
        log.info("插件 {} 卸载了 {} 个 Web 端点", pluginId, removedCount);
    }

    /**
     * 检测路径是否与已有映射冲突。
     *
     * @param mappingInfo 待检测的映射信息
     * @return 如果存在冲突返回 true
     */
    private boolean hasConflict(RequestMappingInfo mappingInfo) {
        try {
            var handlerMethods = handlerMapping.getHandlerMethods();
            for (var entry : handlerMethods.entrySet()) {
                RequestMappingInfo existingInfo = entry.getKey();
                if (mappingInfo.getPatternsCondition() != null
                        && existingInfo.getPatternsCondition() != null) {
                    var newPatterns = mappingInfo.getPatternsCondition().getPatterns();
                    var existingPatterns = existingInfo.getPatternsCondition().getPatterns();
                    for (String newPattern : newPatterns) {
                        if (existingPatterns.contains(newPattern)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("检测路径冲突时发生异常: {}", e.getMessage());
        }
        return false;
    }
}
