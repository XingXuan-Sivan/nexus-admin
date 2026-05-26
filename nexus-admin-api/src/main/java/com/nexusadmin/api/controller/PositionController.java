package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.org.Position;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.PageResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.service.PositionService;
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

/**
 * 岗位管理控制器。
 *
 * <p>提供岗位的增删改查 API。</p>
 */
@RestController
@RequestMapping("/admin/v1/positions")
@Tag(name = "岗位管理")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    /**
     * 获取岗位列表（分页）。
     */
    @GetMapping
    @RequirePermission("users.view")
    @Operation(summary = "获取岗位列表")
    public PageResult<Position> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return positionService.list(page, size);
    }

    /**
     * 获取岗位详情。
     */
    @GetMapping("/{id}")
    @RequirePermission("users.view")
    @Operation(summary = "获取岗位详情")
    public DataResult<Position> get(@PathVariable("id") String id) {
        return DataResult.success(positionService.get(id).orElse(null));
    }

    /**
     * 创建岗位。
     */
    @PostMapping
    @RequirePermission("users.manage")
    @Operation(summary = "创建岗位")
    public DataResult<Position> create(@RequestBody Position position) {
        return DataResult.success(positionService.create(position));
    }

    /**
     * 更新岗位。
     */
    @PutMapping("/{id}")
    @RequirePermission("users.manage")
    @Operation(summary = "更新岗位")
    public DataResult<Position> update(@PathVariable("id") String id,
                                        @RequestBody Position position) {
        return DataResult.success(positionService.update(id, position));
    }

    /**
     * 删除岗位。
     */
    @DeleteMapping("/{id}")
    @RequirePermission("users.manage")
    @Operation(summary = "删除岗位")
    public Result delete(@PathVariable("id") String id) {
        positionService.delete(id);
        return Result.success();
    }
}
