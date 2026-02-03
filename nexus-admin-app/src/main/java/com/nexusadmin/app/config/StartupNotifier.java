package com.nexusadmin.app.config;

import com.nexusadmin.app.config.properties.PlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * 应用启动完成通知器，在所有初始化完成后输出启动成功信息和访问地址。
 */
@Component
@Order(Integer.MAX_VALUE)
public class StartupNotifier implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupNotifier.class);

    private final PlatformProperties platformProperties;
    private final ApplicationContext applicationContext;

    /**
     * 构造启动完成通知器。
     *
     * @param platformProperties 平台配置属性
     * @param applicationContext Spring 应用上下文
     */
    public StartupNotifier(PlatformProperties platformProperties,
                           ApplicationContext applicationContext) {
        this.platformProperties = platformProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 在应用启动完成后输出启动成功信息和访问地址。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            String port = applicationContext.getEnvironment().getProperty("server.port", "8080");
            String contextPath = applicationContext.getEnvironment().getProperty("server.servlet.context-path", "");

            log.info("");
            log.info("===============================================");
            log.info("  {} 启动成功！", platformProperties.getInfo().getName());
            log.info("  版本：{}", platformProperties.getInfo().getVersion());
            log.info("  描述：{}", platformProperties.getInfo().getDescription());
            log.info("-----------------------------------------------");
            log.info("  本地访问地址：http://localhost:{}{}", port, contextPath);
            log.info("  外部访问地址：http://{}:{}{}", host, port, contextPath);
            log.info("===============================================");
            log.info("");
        } catch (Exception e) {
            log.warn("获取访问地址失败", e);
        }
    }
}
