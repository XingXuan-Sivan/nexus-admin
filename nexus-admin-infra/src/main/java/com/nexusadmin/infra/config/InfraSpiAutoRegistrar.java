package com.nexusadmin.infra.config;

import com.nexusadmin.core.spi.SpiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Infra 层 SPI 自动注册器。
 * <p>在应用启动完成后，将 Spring 容器中的基础设施实现自动注册到平台级 {@link SpiRegistry}。</p>
 */
@Component
public class InfraSpiAutoRegistrar implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(InfraSpiAutoRegistrar.class);
    
    private final ApplicationContext applicationContext;
    private final SpiRegistry spiRegistry;

    /**
     * 构造函数，依赖注入 Spring 容器及 SPI 注册中心。
     *
     * @param applicationContext Spring 容器
     * @param spiRegistry        SPI 注册中心
     */
    public InfraSpiAutoRegistrar(ApplicationContext applicationContext, SpiRegistry spiRegistry) {
        this.applicationContext = applicationContext;
        this.spiRegistry = spiRegistry;
    }

    /**
     * 应用启动完成后执行，自动将 Spring 容器中的基础设施实现注册到 SPI 注册中心。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        log.info("-----------------------------------------------");
        log.info("开始加载基础设施 SPI 实现...");
        
        int registeredCount = 0;
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
        for (Object bean : beans.values()) {
            Class<?> beanClass = bean.getClass();
            Class<?>[] interfaces = beanClass.getInterfaces();
            for (Class<?> iface : interfaces) {
                if (!isSpiInterface(iface)) {
                    continue;
                }
                int priority = resolvePriority(bean, beanClass);
                //noinspection unchecked
                spiRegistry.register((Class<Object>) iface, bean, priority);
                registeredCount++;
                log.debug("已注册 SPI 实现：{} -> {} (优先级: {})", 
                         iface.getSimpleName(), beanClass.getSimpleName(), priority);
            }
        }
        
        log.info("基础设施 SPI 实现加载完成：已注册 {} 个 SPI 实现", registeredCount);
        log.info("-----------------------------------------------");
    }

    /**
     * 判断接口是否为 SPI 接口。
     *
     * @param iface 接口类
     * @return 如果是 SPI 接口则返回 true
     */
    private boolean isSpiInterface(Class<?> iface) {
        Package pkg = iface.getPackage();
        if (pkg == null) {
            return false;
        }
        String name = pkg.getName();
        if (!name.startsWith("com.nexusadmin.core.spi")) {
            return false;
        }
        // 避免将 SpiRegistry 自身注册进来
        return !SpiRegistry.class.equals(iface);
    }

    /**
     * 解析 Bean 的优先级。
     *
     * @param bean      Bean 实例
     * @param beanClass Bean 类
     * @return 优先级值
     */
    private int resolvePriority(Object bean, Class<?> beanClass) {
        int priority = 0;
        if (bean instanceof Ordered ordered) {
            priority = ordered.getOrder();
        }
        Order order = beanClass.getAnnotation(Order.class);
        if (order != null) {
            priority = order.value();
        }
        return priority;
    }
}
