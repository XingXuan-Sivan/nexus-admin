package com.nexusadmin.app.listener;

import com.nexusadmin.api.extension.ai.AiTool;
import com.nexusadmin.api.extension.ai.AiToolRegistry;
import com.nexusadmin.api.extension.ai.EnableAiTools;
import com.nexusadmin.api.extension.ai.impl.AiToolAdapter;
import com.nexusadmin.core.PluginState;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.event.EventScopeMatcher;
import com.nexusadmin.core.plugin.event.PluginStateChangedEvent;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具生命周期监听器。
 *
 * <p>镜像 {@link WebEndpointLifecycleListener} 的设计——监听插件状态变更事件，
 * 在插件激活时扫描其包下的 AiTool 实现与 LangChain4j @Tool 注解方法，
 * 自动注册到 {@link AiToolRegistry}；插件停止时自动注销。</p>
 *
 * <p>扫描机制：使用 Spring 的 ClassPathScanningCandidateComponentProvider，
 * 临时切换线程上下文 ClassLoader 到插件 ClassLoader，确保能正确加载插件类。
 * 组件实例化优先使用构造器注入（从 ApplicationContext 获取依赖），
 * 回退到无参构造器。</p>
 *
 * <p>同时支持 LangChain4j @Tool 注解——通过反射扫描 @Tool 注解方法，
 * 自动包装为 {@link AiToolAdapter} 注册到工具注册表。</p>
 */
@Component
public class AiToolLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(AiToolLifecycleListener.class);

    private final EventBus eventBus;
    private final AiToolRegistry toolRegistry;
    private final ApplicationContext applicationContext;

    /**
     * 追踪每个插件注册的工具名称，用于卸载时精确清理。
     * key: 插件ID, value: 该插件注册的工具名称列表
     */
    private final Map<String, List<String>> pluginTools = new ConcurrentHashMap<>();

    /**
     * 构造 AI 工具生命周期监听器。
     *
     * @param eventBus            事件总线
     * @param toolRegistry        AI 工具注册表
     * @param applicationContext  Spring 应用上下文
     */
    public AiToolLifecycleListener(EventBus eventBus,
                                   AiToolRegistry toolRegistry,
                                   ApplicationContext applicationContext) {
        this.eventBus = eventBus;
        this.toolRegistry = toolRegistry;
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化时订阅插件状态变更事件。
     */
    @PostConstruct
    public void init() {
        eventBus.subscribe(
                PluginStateChangedEvent.class,
                this::onStateChanged,
                EventScopeMatcher.platform(),
                null
        );
        log.info("AI 工具生命周期监听器已初始化");
    }

    /**
     * 处理插件状态变更事件。
     *
     * @param event 插件状态变更事件
     */
    private void onStateChanged(PluginStateChangedEvent event) {
        PluginWrapper wrapper = event.plugin();
        String pluginId = wrapper.getPluginId();
        PluginState to = event.to();

        switch (to) {
            case ACTIVE -> scanAndRegister(pluginId, wrapper.plugin());
            case STOPPING -> unregisterPlugin(pluginId);
            default -> {
            }
        }
    }

    /**
     * 扫描插件包并注册 AiTool 与 @Tool 方法。
     *
     * @param pluginId       插件唯一标识
     * @param pluginInstance 插件实例
     */
    private void scanAndRegister(String pluginId, Object pluginInstance) {
        Class<?> pluginClass = pluginInstance.getClass();
        EnableAiTools annotation = pluginClass.getAnnotation(EnableAiTools.class);
        if (annotation == null) {
            return;
        }

        List<String> basePackages = resolveBasePackages(annotation, pluginClass);
        ClassLoader pluginClassLoader = pluginClass.getClassLoader();

        List<String> registeredNames = new ArrayList<>();
        for (String basePackage : basePackages) {
            Set<Class<?>> candidates = scanPackage(basePackage, pluginClassLoader);
            log.debug("插件 {} 在包 {} 扫描到 {} 个候选 AI 组件", pluginId, basePackage, candidates.size());

            for (Class<?> clazz : candidates) {
                try {
                    Object instance = instantiate(clazz);
                    List<String> names = registerTools(instance);
                    registeredNames.addAll(names);
                } catch (Exception e) {
                    log.error("注册 AI 工具失败 (插件: {}, 类: {}): {}",
                            pluginId, clazz.getName(), e.getMessage(), e);
                }
            }
        }

        if (!registeredNames.isEmpty()) {
            pluginTools.put(pluginId, registeredNames);
            log.info("插件 {} 注册了 {} 个 AI 工具: {}", pluginId, registeredNames.size(), registeredNames);
        }
    }

    /**
     * 解析要扫描的包路径。
     * <p>未指定时默认使用插件主类所在包。</p>
     *
     * @param annotation  注解实例
     * @param pluginClass 插件主类
     * @return 基础包路径列表
     */
    private List<String> resolveBasePackages(EnableAiTools annotation, Class<?> pluginClass) {
        List<String> packages = new ArrayList<>();
        for (String pkg : annotation.basePackages()) {
            if (pkg != null && !pkg.isBlank()) {
                packages.add(pkg);
            }
        }
        for (Class<?> cls : annotation.basePackageClasses()) {
            String pkg = cls.getPackageName();
            if (pkg != null && !pkg.isBlank()) {
                packages.add(pkg);
            }
        }
        if (packages.isEmpty()) {
            packages.add(pluginClass.getPackageName());
        }
        return packages;
    }

    /**
     * 扫描指定包下的候选组件（@Component 标注的类）。
     * <p>临时切换线程上下文 ClassLoader 到插件 ClassLoader，
     * 确保 ClassPathScanningCandidateComponentProvider 能正确加载插件类。</p>
     *
     * @param basePackage       基础包名
     * @param pluginClassLoader 插件类加载器
     * @return 候选组件类集合
     */
    private Set<Class<?>> scanPackage(String basePackage, ClassLoader pluginClassLoader) {
        Set<Class<?>> candidates = new java.util.HashSet<>();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginClassLoader);

            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

            Set<BeanDefinition> definitions = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition def : definitions) {
                try {
                    Class<?> clazz = Class.forName(def.getBeanClassName(), false, pluginClassLoader);
                    candidates.add(clazz);
                } catch (ClassNotFoundException e) {
                    log.warn("无法加载候选组件类: {}", def.getBeanClassName());
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
        return candidates;
    }

    /**
     * 实例化组件。
     * <p>优先使用构造器注入（从 ApplicationContext 获取依赖参数），
     * 回退到无参构造器。</p>
     *
     * @param clazz 组件类
     * @return 组件实例
     * @throws Exception 实例化失败时抛出
     */
    private Object instantiate(Class<?> clazz) throws Exception {
        for (Constructor<?> ctor : clazz.getConstructors()) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            boolean allResolved = true;
            for (int i = 0; i < paramTypes.length; i++) {
                try {
                    args[i] = applicationContext.getBean(paramTypes[i]);
                } catch (Exception e) {
                    allResolved = false;
                    break;
                }
            }
            if (allResolved) {
                return ctor.newInstance(args);
            }
        }
        return clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * 注册工具。
     * <p>如果实例本身是 AiTool，直接注册；否则尝试提取 @Tool 注解方法。</p>
     *
     * @param instance 组件实例
     * @return 已注册的工具名称列表
     */
    private List<String> registerTools(Object instance) {
        List<String> names = new ArrayList<>();

        if (instance instanceof AiTool aiTool) {
            toolRegistry.register(aiTool);
            names.add(aiTool.getName());
            return names;
        }

        List<AiTool> toolMethods = extractToolMethods(instance);
        for (AiTool tool : toolMethods) {
            toolRegistry.register(tool);
            names.add(tool.getName());
        }
        return names;
    }

    /**
     * 从对象中提取 LangChain4j @Tool 注解方法，包装为 AiTool。
     * <p>直接扫描 {@code dev.langchain4j.agent.tool.Tool} 注解的方法，
     * 通过 {@link AiToolAdapter} 包装为 AiTool。</p>
     *
     * @param instance 组件实例
     * @return 提取出的 AiTool 列表
     */
    private List<AiTool> extractToolMethods(Object instance) {
        List<AiTool> tools = new ArrayList<>();
        Class<?> clazz = instance.getClass();

        // 尝试加载 LangChain4j @Tool 注解
        Class<? extends java.lang.annotation.Annotation> toolAnnotation;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> annotationClass =
                    (Class<? extends java.lang.annotation.Annotation>)
                            Class.forName("dev.langchain4j.agent.tool.Tool");
            toolAnnotation = annotationClass;
        } catch (ClassNotFoundException e) {
            log.debug("LangChain4j @Tool 注解不可用，跳过工具方法扫描");
            return tools;
        }

        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(toolAnnotation)) {
                try {
                    AiToolAdapter tool = new AiToolAdapter(instance, method);
                    tools.add(tool);
                    log.debug("扫描到 @Tool 方法: {}.{}", clazz.getSimpleName(), method.getName());
                } catch (Exception ex) {
                    log.debug("无法为方法 {} 创建 AiToolAdapter: {}",
                            method.getName(), ex.getMessage());
                }
            }
        }

        return tools;
    }

    /**
     * 卸载插件注册的所有工具。
     *
     * @param pluginId 插件唯一标识
     */
    private void unregisterPlugin(String pluginId) {
        List<String> names = pluginTools.remove(pluginId);
        if (names == null || names.isEmpty()) {
            return;
        }
        for (String name : names) {
            toolRegistry.unregister(name);
        }
        log.info("插件 {} 卸载了 {} 个 AI 工具", pluginId, names.size());
    }
}
