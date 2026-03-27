package com.nexusadmin.app.listener;

import com.nexusadmin.api.extension.web.MappingResolver;
import com.nexusadmin.api.extension.web.PluginWebRegistry;
import com.nexusadmin.api.extension.web.WebControllerProvider;
import com.nexusadmin.api.extension.web.WebEndpointExtension;
import com.nexusadmin.api.extension.web.WebEndpointRegistrar;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.EventScopeMatcher;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.event.PluginStateChangedEvent;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Web 端点生命周期监听器。
 * <p>
 * 负责在插件生命周期事件触发时，通过 WebEndpointExtension 扩展点
 * 自动注册或卸载插件的 Web 端点。
 */
@Component
public class WebEndpointLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(WebEndpointLifecycleListener.class);

    private final EventBus eventBus;
    private final ExtensionRegistry extensionRegistry;
    private final WebEndpointRegistrar registrar;
    private final MappingResolver mappingResolver;
    private final PluginWebRegistry registry;

    /**
     * 构造 Web 端点生命周期监听器。
     *
     * @param eventBus          事件总线
     * @param extensionRegistry 扩展注册中心
     * @param registrar         Web 端点注册器
     * @param mappingResolver   映射解析器
     * @param registry          插件 Web 端点注册表
     */
    public WebEndpointLifecycleListener(EventBus eventBus,
                                        ExtensionRegistry extensionRegistry,
                                        WebEndpointRegistrar registrar,
                                        MappingResolver mappingResolver,
                                        PluginWebRegistry registry) {
        this.eventBus = eventBus;
        this.extensionRegistry = extensionRegistry;
        this.registrar = registrar;
        this.mappingResolver = mappingResolver;
        this.registry = registry;
    }

    /**
     * 初始化时订阅生命周期事件。
     */
    @PostConstruct
    public void init() {
        eventBus.subscribe(
                PluginStateChangedEvent.class,
                this::onStateChanged,
                EventScopeMatcher.platform(),
                null
        );
        log.info("Web 端点生命周期监听器已初始化");
    }

    /**
     * 处理插件状态变更事件。
     *
     * @param event 插件状态变更事件
     */
    private void onStateChanged(PluginStateChangedEvent event) {
        PluginWrapper pluginWrapper = event.plugin();
        String pluginId = pluginWrapper.getPluginId();
        PluginState to = event.to();

        Object plugin = pluginWrapper.plugin();
        WebControllerProvider provider = (plugin instanceof WebControllerProvider p) ? p : null;

        WebEndpointExtension.Context ctx = new WebEndpointExtension.Context(
                pluginId,
                plugin,
                provider,
                registrar,
                mappingResolver,
                registry
        );

        List<WebEndpointExtension> extensions = extensionRegistry.getAll(WebEndpointExtension.class);

        switch (to) {
            case ACTIVE -> extensions.forEach(ext -> {
                try {
                    ext.registerEndpoints(ctx);
                } catch (Exception e) {
                    log.error("通过扩展点注册 Web 端点失败 (插件: {}): {}", pluginId, e.getMessage(), e);
                }
            });
            case STOPPING -> extensions.forEach(ext -> {
                try {
                    ext.unregisterEndpoints(ctx);
                } catch (Exception e) {
                    log.error("通过扩展点卸载 Web 端点失败 (插件: {}): {}", pluginId, e.getMessage(), e);
                }
            });
            default -> {
            }
        }
    }
}
