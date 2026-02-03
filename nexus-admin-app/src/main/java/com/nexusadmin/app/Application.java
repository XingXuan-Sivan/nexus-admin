package com.nexusadmin.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用入口类，启动 Spring Boot 应用并扫描 nexus-admin 各模块的 Spring Bean。
 */
@SpringBootApplication(scanBasePackages = "com.nexusadmin")
public class Application {
    /**
     * 应用启动入口方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
