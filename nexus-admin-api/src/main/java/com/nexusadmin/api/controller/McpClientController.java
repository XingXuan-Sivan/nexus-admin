package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.ai.McpConnectionInfo;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.ai.McpClientConnection;
import com.nexusadmin.api.service.McpClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * MCP 客户端管理控制器。
 *
 * <p>提供外部 MCP 服务连接的 CRUD 管理、连接测试、远程工具浏览与调用、
 * 以及桥接管理（刷新/查询）等 API。
 * 与 {@code McpController}（MCP Server 端点）共同构成平台 MCP 双向通信能力。</p>
 *
 * <p>API 路径体系：
 * <ul>
 *   <li>{@code /admin/v1/mcp} — MCP 服务端端点（对外暴露 AiTool）</li>
 *   <li>{@code /admin/v1/mcp/clients} — MCP 客户端管理 API（管理外部 MCP 连接）</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/v1/mcp/clients")
@Tag(name = "MCP 客户端管理")
public class McpClientController {

    private final McpClientService mcpClientService;

    public McpClientController(McpClientService mcpClientService) {
        this.mcpClientService = mcpClientService;
    }

    /**
     * 获取所有 MCP 客户端连接列表。
     *
     * @return 连接配置列表
     */
    @GetMapping
    @RequirePermission("mcp-clients.view")
    @Operation(summary = "获取 MCP 客户端连接列表")
    public DataResult<List<McpConnectionInfo>> list() {
        return DataResult.success(mcpClientService.list());
    }

    /**
     * 获取指定连接详情。
     *
     * @param id 连接唯一标识
     * @return 连接配置
     */
    @GetMapping("/{id}")
    @RequirePermission("mcp-clients.view")
    @Operation(summary = "获取 MCP 客户端连接详情")
    public DataResult<McpConnectionInfo> get(@PathVariable("id") String id) {
        return DataResult.success(mcpClientService.get(id));
    }

    /**
     * 创建新的 MCP 客户端连接。
     *
     * @param info 连接配置信息
     * @return 创建后的连接配置
     */
    @PostMapping
    @RequirePermission("mcp-clients.manage")
    @Operation(summary = "创建 MCP 客户端连接")
    public DataResult<McpConnectionInfo> create(@RequestBody McpConnectionInfo info) {
        return DataResult.success(mcpClientService.create(info));
    }

    /**
     * 更新已有连接配置。
     *
     * @param id   连接唯一标识
     * @param info 新的连接配置
     * @return 更新后的连接配置
     */
    @PutMapping("/{id}")
    @RequirePermission("mcp-clients.manage")
    @Operation(summary = "更新 MCP 客户端连接")
    public DataResult<McpConnectionInfo> update(@PathVariable("id") String id,
                                                 @RequestBody McpConnectionInfo info) {
        return DataResult.success(mcpClientService.update(id, info));
    }

    /**
     * 删除指定连接。
     *
     * @param id 连接唯一标识
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RequirePermission("mcp-clients.manage")
    @Operation(summary = "删除 MCP 客户端连接")
    public Result delete(@PathVariable("id") String id) {
        mcpClientService.delete(id);
        return Result.success();
    }

    /**
     * 测试连接可用性。
     *
     * @param id   连接唯一标识
     * @param info 连接配置（用于测试未保存的连接）
     * @return 测试结果
     */
    @PostMapping("/{id}/test")
    @RequirePermission("mcp-clients.view")
    @Operation(summary = "测试 MCP 客户端连接")
    public DataResult<Boolean> testConnection(@PathVariable("id") String id,
                                               @RequestBody McpConnectionInfo info) {
        return DataResult.success(mcpClientService.testConnection(info));
    }

    /**
     * 列出指定连接的所有远程工具。
     *
     * @param id 连接唯一标识
     * @return 远程工具列表
     */
    @GetMapping("/{id}/tools")
    @RequirePermission("mcp-clients.view")
    @Operation(summary = "获取远程工具列表")
    public DataResult<List<McpClientConnection.McpRemoteTool>> listRemoteTools(
            @PathVariable("id") String id) {
        return DataResult.success(mcpClientService.listRemoteTools(id));
    }

    /**
     * 调用指定连接的远程工具。
     *
     * @param id       连接唯一标识
     * @param params   toolName 和 arguments
     * @return 调用结果
     */
    @PostMapping("/{id}/tools/call")
    @RequirePermission("mcp-clients.view")
    @Operation(summary = "调用远程工具")
    public DataResult<McpClientConnection.McpToolResult> callRemoteTool(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> params) {
        String toolName = (String) params.get("toolName");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
        return DataResult.success(mcpClientService.callRemoteTool(toolName, arguments));
    }

    /**
     * 刷新指定连接的远程工具桥接（断开旧桥接，重新拉取并注册）。
     *
     * @param id 连接唯一标识
     * @return 刷新的工具数量
     */
    @PostMapping("/{id}/bridge/refresh")
    @RequirePermission("mcp-clients.manage")
    @Operation(summary = "刷新远程工具桥接")
    public DataResult<Integer> refreshBridge(@PathVariable("id") String id) {
        return DataResult.success(mcpClientService.refreshBridge(id));
    }

    /**
     * 获取指定连接已桥接的工具名称列表。
     *
     * @param id 连接唯一标识
     * @return 已桥接的工具名称列表
     */
    @GetMapping("/{id}/bridged-tools")
    @RequirePermission("mcp-clients.view")
    @Operation(summary = "获取已桥接工具名称列表")
    public DataResult<List<String>> getBridgedTools(@PathVariable("id") String id) {
        return DataResult.success(mcpClientService.getBridgedTools(id));
    }
}
