package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.dictionary.Dictionary;
import com.nexusadmin.api.domain.dictionary.DictionaryItem;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典管理控制器。
 *
 * <p>提供字典及字典项的增删改查 API。</p>
 */
@RestController
@RequestMapping("/admin/v1/dictionaries")
@Tag(name = "字典管理")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    /**
     * 获取字典列表（分页）。
     */
    @GetMapping
    @RequirePermission("config.view")
    @Operation(summary = "获取字典列表")
    public PageResult<Dictionary> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return dictionaryService.list(page, size);
    }

    /**
     * 获取字典详情。
     */
    @GetMapping("/{code}")
    @RequirePermission("config.view")
    @Operation(summary = "获取字典详情")
    public DataResult<Dictionary> get(@PathVariable("code") String code) {
        return DataResult.success(dictionaryService.get(code).orElse(null));
    }

    /**
     * 创建字典。
     */
    @PostMapping
    @RequirePermission("config.manage")
    @Operation(summary = "创建字典")
    public DataResult<Dictionary> create(@RequestBody Dictionary dict) {
        return DataResult.success(dictionaryService.create(dict));
    }

    /**
     * 更新字典。
     */
    @PutMapping("/{code}")
    @RequirePermission("config.manage")
    @Operation(summary = "更新字典")
    public DataResult<Dictionary> update(@PathVariable("code") String code,
                                          @RequestBody Dictionary dict) {
        return DataResult.success(dictionaryService.update(code, dict));
    }

    /**
     * 删除字典。
     */
    @DeleteMapping("/{code}")
    @RequirePermission("config.manage")
    @Operation(summary = "删除字典")
    public Result delete(@PathVariable("code") String code) {
        dictionaryService.delete(code);
        return Result.success();
    }

    // ==================== 字典项管理 ====================

    /**
     * 获取字典项列表。
     */
    @GetMapping("/{dictCode}/items")
    @RequirePermission("config.view")
    @Operation(summary = "获取字典项列表")
    public DataResult<List<DictionaryItem>> listItems(@PathVariable("dictCode") String dictCode) {
        return DataResult.success(dictionaryService.listItems(dictCode));
    }

    /**
     * 创建字典项。
     */
    @PostMapping("/{dictCode}/items")
    @RequirePermission("config.manage")
    @Operation(summary = "创建字典项")
    public DataResult<DictionaryItem> createItem(@PathVariable("dictCode") String dictCode,
                                                   @RequestBody DictionaryItem item) {
        return DataResult.success(dictionaryService.createItem(dictCode, item));
    }

    /**
     * 更新字典项。
     */
    @PutMapping("/{dictCode}/items/{itemCode}")
    @RequirePermission("config.manage")
    @Operation(summary = "更新字典项")
    public DataResult<DictionaryItem> updateItem(@PathVariable("dictCode") String dictCode,
                                                   @PathVariable("itemCode") String itemCode,
                                                   @RequestBody DictionaryItem item) {
        return DataResult.success(dictionaryService.updateItem(dictCode, itemCode, item));
    }

    /**
     * 删除字典项。
     */
    @DeleteMapping("/{dictCode}/items/{itemCode}")
    @RequirePermission("config.manage")
    @Operation(summary = "删除字典项")
    public Result deleteItem(@PathVariable("dictCode") String dictCode,
                             @PathVariable("itemCode") String itemCode) {
        dictionaryService.deleteItem(dictCode, itemCode);
        return Result.success();
    }
}
