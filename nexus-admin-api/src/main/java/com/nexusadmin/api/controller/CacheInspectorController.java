package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.extension.cache.CacheProvider;
import com.nexusadmin.api.extension.cache.CacheProvider.CacheKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 缓存检查控制器。
 *
 * <p>提供缓存数据查看与清除 API，委托 CacheProvider 扩展点实现。</p>
 */
@RestController
@RequestMapping("/admin/v1/cache")
@Tag(name = "缓存检查")
public class CacheInspectorController {

    private final CacheProvider cacheProvider;

    public CacheInspectorController(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    /**
     * 查看缓存条目。
     *
     * @param namespace 命名空间
     * @param key       缓存键
     * @return 缓存条目视图
     */
    @GetMapping("/{namespace}/{key}")
    @RequirePermission("system.view")
    @Operation(summary = "查看缓存条目")
    public DataResult<Map<String, Object>> view(@PathVariable("namespace") String namespace,
                                                 @PathVariable("key") String key) {
        CacheKey cacheKey = new CacheKey(namespace, key);
        InvocationContext ctx = InvocationContext.builder().build();
        return cacheProvider.get(cacheKey, ctx)
                .map(val -> {
                    String encoded = Base64.getEncoder().encodeToString(val.payload());
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("namespace", cacheKey.namespace());
                    result.put("key", cacheKey.key());
                    result.put("ttlSeconds", val.ttlSeconds());
                    result.put("size", val.payload().length);
                    result.put("payloadBase64", encoded);
                    result.put("metadata", val.metadata());
                    return DataResult.success(result);
                })
                .orElseGet(() -> DataResult.success(Collections.emptyMap()));
    }

    /**
     * 清除缓存条目。
     *
     * @param namespace 命名空间
     * @param key       缓存键
     * @return 操作结果
     */
    @DeleteMapping("/{namespace}/{key}")
    @RequirePermission("config.manage")
    @Operation(summary = "清除缓存条目")
    public Result evict(@PathVariable("namespace") String namespace,
                        @PathVariable("key") String key) {
        CacheKey cacheKey = new CacheKey(namespace, key);
        InvocationContext ctx = InvocationContext.builder().build();
        cacheProvider.evict(cacheKey, ctx);
        return Result.success();
    }
}
