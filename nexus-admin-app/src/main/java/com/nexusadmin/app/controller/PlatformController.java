package com.nexusadmin.app.controller;

import com.nexusadmin.api.result.DataResult;
import com.nexusadmin.core.config.ConfigManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台默认控制器，提供平台基本信息查询和健康检查接口。
 */
@RestController
public class PlatformController {

    private final ConfigManager configManager;

    /**
     * 构造平台控制器。
     *
     * @param configManager 配置管理器
     */
    public PlatformController(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 平台默认接口，返回平台基本信息。
     * <p>用于检查项目是否启动成功。</p>
     *
     * @return 平台基本信息
     */
    @GetMapping("/")
    public DataResult<Map<String, Object>> index() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", configManager.get("platform", "infoName").orElse("Nexus Admin"));
        data.put("version", configManager.get("platform", "infoVersion").orElse("0.1.0-SNAPSHOT"));
        data.put("description", configManager.get("platform", "infoDescription").orElse("插件化系统拓展平台"));
        data.put("status", "running");
        return DataResult.success(data);
    }
}
