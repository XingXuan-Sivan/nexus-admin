package com.nexusadmin.app.config;

import com.nexusadmin.core.config.ConfigManager;
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

    private final ConfigManager configManager;
    private final ApplicationContext applicationContext;

    /**
     * 构造启动完成通知器。
     *
     * @param configManager      配置管理器
     * @param applicationContext Spring 应用上下文
     */
    public StartupNotifier(ConfigManager configManager,
                           ApplicationContext applicationContext) {
        this.configManager = configManager;
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

            String name = configManager.get("platform", "infoName").orElse("Nexus Admin");
            String version = configManager.get("platform", "infoVersion").orElse("0.1.0-SNAPSHOT");
            String description = configManager.get("platform", "infoDescription").orElse("插件化系统拓展平台");

            log.info("");
            log.info("===============================================");
            log.info("  {} 启动成功！", name);
            log.info("  版本：{}", version);
            log.info("  描述：{}", description);
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
