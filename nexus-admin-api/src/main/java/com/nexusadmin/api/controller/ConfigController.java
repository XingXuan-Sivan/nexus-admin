package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.ConfigService;
import com.nexusadmin.core.facade.ConfigFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 配置管理控制器。
 * <p>
 * 提供配置域列表、Schema 查询、配置值读写等 API。
 */
@RestController
@RequestMapping("/admin/v1/config")
@Tag(name = "配置管理")
public class ConfigController {

    private final ConfigService configService;

    private final ConfigFacade configFacade;

    /**
     * 构造配置管理控制器。
     *
     * @param configService 配置管理服务
     * @param configFacade  核心配置管理门面
     */
    public ConfigController(ConfigService configService, ConfigFacade configFacade) {
        this.configService = configService;
        this.configFacade = configFacade;
    }

    /**
     * 获取配置域列表。
     *
     * @return 配置域标识列表
     */
    @GetMapping("/scopes")
    @RequirePermission("config.view")
    @Operation(summary = "获取配置域列表")
    public DataResult<List<String>> listScopes() {
        return DataResult.success(configService.getScopes());
    }

    /**
     * 获取指定配置域的 JSON Schema。
     *
     * @param scope 配置域标识
     * @return JSON Schema 映射
     */
    @GetMapping("/{scope}/schema")
    @RequirePermission("config.view")
    @Operation(summary = "获取配置域 Schema")
    public DataResult<Map<String, Object>> getSchema(@PathVariable("scope") String scope) {
        return DataResult.success(configService.getSchema(scope).orElse(null));
    }

    /**
     * 获取指定配置域的配置值。
     *
     * @param scope 配置域标识
     * @return 配置键值映射
     */
    @GetMapping("/{scope}")
    @RequirePermission("config.view")
    @Operation(summary = "获取配置值")
    public DataResult<Map<String, String>> getConfig(@PathVariable("scope") String scope) {
        return DataResult.success(configService.getConfig(scope));
    }

    /**
     * 更新指定配置域的配置值。
     *
     * @param scope  配置域标识
     * @param values 配置键值映射
     * @return 操作结果
     */
    @PutMapping("/{scope}")
    @RequirePermission("config.manage")
    @Operation(summary = "更新配置值")
    @SuppressWarnings("unchecked")
    public Result updateConfig(@PathVariable("scope") String scope,
                               @RequestBody Map<String, Object> body) {
        // 前端发送格式: { "values": { "key1": "val1", ... } }
        Object valuesObj = body.get("values");
        if (valuesObj instanceof Map<?, ?> rawMap) {
            Map<String, String> values = new java.util.HashMap<>();
            rawMap.forEach((k, v) -> values.put(String.valueOf(k), v != null ? v.toString() : null));
            configService.updateConfig(scope, values);
        }
        return Result.success();
    }

    /**
     * 重置指定配置域为默认值。
     *
     * @param scope 配置域标识
     * @return 操作结果
     */
    @PostMapping("/{scope}/reset")
    @RequirePermission("config.manage")
    @Operation(summary = "重置配置为默认值")
    @SuppressWarnings("unchecked")
    public Result resetConfig(@PathVariable("scope") String scope,
                              @RequestBody(required = false) Map<String, Object> body) {
        // 前端发送格式: { "keys": ["key1", "key2"] } 或 {}
        if (body != null && body.get("keys") instanceof List<?> keyList) {
            for (Object key : keyList) {
                configFacade.remove(scope, String.valueOf(key));
            }
        } else {
            configService.resetConfig(scope);
        }
        return Result.success();
    }
}
