package com.nexusadmin.app.controller;

import com.nexusadmin.api.result.DataResult;
import com.nexusadmin.app.config.properties.PlatformProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台默认控制器，提供平台基本信息查询和健康检查接口。
 */
@RestController
public class PlatformController {

    private final PlatformProperties platformProperties;

    /**
     * 构造平台控制器。
     *
     * @param platformProperties 平台配置属性
     */
    public PlatformController(PlatformProperties platformProperties) {
        this.platformProperties = platformProperties;
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
        data.put("name", platformProperties.getInfo().getName());
        data.put("version", platformProperties.getInfo().getVersion());
        data.put("description", platformProperties.getInfo().getDescription());
        data.put("status", "running");
        return DataResult.success(data);
    }
}
