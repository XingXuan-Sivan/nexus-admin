package com.nexusadmin.app.web;

import com.nexusadmin.api.web.EnableWebEndpoints;
import com.nexusadmin.api.web.WebControllerProvider;
import com.nexusadmin.api.web.WebEndpointExtension;
import com.nexusadmin.core.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * Spring MVC 下的 Web 接入扩展点实现。
 * <p>
 * 负责在插件激活时，将插件提供的控制器注册到 Spring MVC，
 * 并在插件停止前卸载所有已注册的端点。
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>显式提供：通过 {@link WebControllerProvider} 接口显式提供控制器</li>
 *   <li>自动扫描：通过 {@link EnableWebEndpoints}
 *       注解启用自动扫描</li>
 * </ul>
 */
@Extension(priority = 50)
public class SpringWebEndpointExtension implements WebEndpointExtension {

    private static final Logger log = LoggerFactory.getLogger(SpringWebEndpointExtension.class);

    private final ApplicationContext applicationContext;

    /**
     * 构造 Spring Web 端点扩展点实现。
     *
     * @param applicationContext Spring 应用上下文
     */
    public SpringWebEndpointExtension(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void registerEndpoints(Context ctx) {
        // 1. 处理显式提供的控制器（兼容旧模式）
        registerExplicitControllers(ctx);

        // 2. 处理自动扫描的控制器（新模式）
        registerScannedControllers(ctx);
    }

    @Override
    public void unregisterEndpoints(Context ctx) {
        ctx.registrar().unregister(ctx.pluginId());
    }

    /**
     * 注册显式提供的控制器。
     *
     * @param ctx 注册上下文
     */
    private void registerExplicitControllers(Context ctx) {
        WebControllerProvider provider = ctx.controllerProvider();
        if (provider == null) {
            return;
        }

        for (Object controller : provider.getControllers()) {
            ctx.registrar().register(ctx.pluginId(), controller);
        }
    }

    /**
     * 注册通过注解自动扫描的控制器。
     *
     * @param ctx 注册上下文
     */
    private void registerScannedControllers(Context ctx) {
        EndpointScanConfig scanConfig = ctx.scanConfig();
        if (!scanConfig.enabled()) {
            return;
        }

        String pluginId = ctx.pluginId();
        Object plugin = ctx.plugin();
        ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();

        log.debug("开始扫描插件 Web 端点 (插件: {}, 包: {})", pluginId, String.join(", ", scanConfig.basePackages()));

        for (String basePackage : scanConfig.basePackages()) {
            try {
                Set<Class<?>> controllerClasses = scanPackage(basePackage, pluginClassLoader);
                log.debug("扫描到 {} 个控制器类 (插件: {}, 包: {})", controllerClasses.size(), pluginId, basePackage);

                for (Class<?> controllerClass : controllerClasses) {
                    try {
                        Object controller = instantiateController(controllerClass);
                        ctx.registrar().register(pluginId, controller);
                        log.debug("注册 Web 控制器成功 (插件: {}, 类: {})", pluginId, controllerClass.getName());
                    } catch (Exception e) {
                        log.error("实例化或注册 Web 控制器失败 (插件: {}, 类: {}): {}",
                                pluginId, controllerClass.getName(), e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                log.error("扫描包失败 (插件: {}, 包: {}): {}", pluginId, basePackage, e.getMessage(), e);
            }
        }
    }

    /**
     * 扫描指定包下的控制器类。
     *
     * @param basePackage       基础包名
     * @param pluginClassLoader 插件类加载器
     * @return 控制器类集合
     */
    private Set<Class<?>> scanPackage(String basePackage, ClassLoader pluginClassLoader) {
        Set<Class<?>> controllers = new java.util.HashSet<>();

        // 保存原始类加载器
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 设置线程上下文类加载器为插件类加载器，使扫描器能正确加载插件类
            Thread.currentThread().setContextClassLoader(pluginClassLoader);

            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
            scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            log.debug("在包 {} 下扫描到 {} 个候选组件", basePackage, candidates.size());

            for (BeanDefinition beanDefinition : candidates) {
                try {
                    Class<?> clazz = Class.forName(beanDefinition.getBeanClassName(), false, pluginClassLoader);
                    controllers.add(clazz);
                } catch (ClassNotFoundException e) {
                    log.warn("无法加载控制器类: {}", beanDefinition.getBeanClassName());
                }
            }
        } finally {
            // 恢复原始类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return controllers;
    }

    /**
     * 实例化控制器，优先使用构造器注入。
     * <p>
     * 尝试查找所有参数均可从 ApplicationContext 获取的构造器，
     * 若找不到则回退到无参构造器。
     *
     * @param controllerClass 控制器类
     * @return 控制器实例
     * @throws Exception 实例化失败时抛出
     */
    private Object instantiateController(Class<?> controllerClass) throws Exception {
        for (Constructor<?> constructor : controllerClass.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            boolean allParamsResolvable = true;

            for (int i = 0; i < paramTypes.length; i++) {
                try {
                    args[i] = applicationContext.getBean(paramTypes[i]);
                } catch (Exception e) {
                    allParamsResolvable = false;
                    break;
                }
            }

            if (allParamsResolvable) {
                return constructor.newInstance(args);
            }
        }

        // 回退到无参构造器
        return controllerClass.getDeclaredConstructor().newInstance();
    }
}
