package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.storage.StorageProvider;
import com.nexusadmin.api.storage.StorageProvider.StorageKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * 存储浏览控制器。
 *
 * <p>提供存储对象的查看与删除 API，委托 StorageProvider 扩展点实现。</p>
 */
@RestController
@RequestMapping("/admin/v1/storage")
@Tag(name = "存储浏览")
public class StorageBrowserController {

    private final StorageProvider storageProvider;

    public StorageBrowserController(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    /**
     * 查看存储对象。
     *
     * @param namespace 命名空间
     * @param key       存储键
     * @return 存储对象视图
     */
    @GetMapping("/{namespace}/{key}")
    @RequirePermission("system.view")
    @Operation(summary = "查看存储对象")
    public DataResult<Map<String, Object>> view(@PathVariable("namespace") String namespace,
                                                 @PathVariable("key") String key) {
        StorageKey storageKey = new StorageKey(namespace, key);
        InvocationContext ctx = InvocationContext.builder().build();
        return storageProvider.load(storageKey, ctx)
                .map(obj -> {
                    Map<String, Object> result = new java.util.LinkedHashMap<>();
                    result.put("namespace", obj.key().namespace());
                    result.put("key", obj.key().key());
                    result.put("contentType", obj.contentType());
                    result.put("size", obj.payload().length);
                    result.put("payloadBase64", Base64.getEncoder().encodeToString(obj.payload()));
                    result.put("metadata", obj.metadata());
                    return DataResult.success(result);
                })
                .orElseGet(() -> DataResult.success(Collections.emptyMap()));
    }

    /**
     * 删除存储对象。
     *
     * @param namespace 命名空间
     * @param key       存储键
     * @return 操作结果
     */
    @DeleteMapping("/{namespace}/{key}")
    @RequirePermission("config.manage")
    @Operation(summary = "删除存储对象")
    public Result delete(@PathVariable("namespace") String namespace,
                         @PathVariable("key") String key) {
        StorageKey storageKey = new StorageKey(namespace, key);
        InvocationContext ctx = InvocationContext.builder().build();
        storageProvider.delete(storageKey, ctx);
        return Result.success();
    }
}
