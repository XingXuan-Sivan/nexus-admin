package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.view.PluginDetailView;
import com.nexusadmin.api.domain.view.PluginStateView;
import com.nexusadmin.api.domain.view.PluginView;
import com.nexusadmin.api.domain.view.PluginConfigurationView;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.domain.result.StatusCodes;
import com.nexusadmin.api.service.PluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 插件管理控制器。
 * <p>
 * 提供插件列表查询、启停控制等管理 API。
 * 作为平台内建能力，直接映射到 /admin/v1/plugins 路径。
 */
@RestController
@RequestMapping("/admin/v1/plugins")
@Tag(name = "插件管理")
public class PluginController {

    private static final Logger log = LoggerFactory.getLogger(PluginController.class);

    private final PluginService pluginService;

    private final Path pluginDir;

    /**
     * 构造插件管理控制器。
     *
     * @param pluginService 插件管理服务
     * @param pluginPath    插件根目录路径（来自 plugin.path 配置）
     */
    public PluginController(PluginService pluginService,
                            @Value("${plugin.path:plugins}") String pluginPath) {
        this.pluginService = pluginService;
        this.pluginDir = Paths.get(pluginPath).toAbsolutePath().normalize();
    }

    /**
     * 获取所有插件列表。
     *
     * @return 插件摘要列表
     */
    @GetMapping
    @RequirePermission("plugins.view")
    @Operation(summary = "获取插件列表")
    public DataResult<List<PluginView>> listAll() {
        return DataResult.success(pluginService.listAll());
    }

    /**
     * 获取插件详情。
     *
     * @param pluginId 插件标识
     * @return 插件详情
     */
    @GetMapping("/{pluginId}")
    @RequirePermission("plugins.view")
    @Operation(summary = "获取插件详情")
    public DataResult<PluginDetailView> getDetail(@PathVariable("pluginId") String pluginId) {
        return DataResult.success(pluginService.getDetail(pluginId).orElse(null));
    }

    /**
     * 启动插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/start")
    @RequirePermission("plugins.manage")
    @Operation(summary = "启动插件")
    public Result start(@PathVariable("pluginId") String pluginId) {
        pluginService.start(pluginId);
        return Result.success();
    }

    /**
     * 停止插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/stop")
    @RequirePermission("plugins.manage")
    @Operation(summary = "停止插件")
    public Result stop(@PathVariable("pluginId") String pluginId) {
        pluginService.stop(pluginId);
        return Result.success();
    }

    /**
     * 启用插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/enable")
    @RequirePermission("plugins.manage")
    @Operation(summary = "启用插件")
    public Result enable(@PathVariable("pluginId") String pluginId) {
        pluginService.enable(pluginId);
        return Result.success();
    }

    /**
     * 禁用插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/disable")
    @RequirePermission("plugins.manage")
    @Operation(summary = "禁用插件")
    public Result disable(@PathVariable("pluginId") String pluginId) {
        pluginService.disable(pluginId);
        return Result.success();
    }

    /**
     * 卸载插件。
     *
     * @param pluginId 插件标识
     * @return 操作结果
     */
    @DeleteMapping("/{pluginId}")
    @RequirePermission("plugins.manage")
    @Operation(summary = "卸载插件")
    public Result unload(@PathVariable("pluginId") String pluginId) {
        pluginService.unload(pluginId);
        return Result.success();
    }

    /**
     * 上传插件 JAR 包。
     * <p>将上传的 JAR 文件保存到插件目录，插件将在下次服务重启后被自动发现和加载。</p>
     *
     * @param file 上传的 JAR 文件
     * @return 上传结果，包含基本插件信息
     */
    @PostMapping("/upload")
    @RequirePermission("plugins.upload")
    @Operation(summary = "上传插件 JAR")
    public DataResult<PluginView> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        // 校验文件不为空
        if (file == null || file.isEmpty()) {
            return DataResult.of(StatusCodes.BAD_REQUEST, "上传文件不能为空", null);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".jar")) {
            return DataResult.of(StatusCodes.BAD_REQUEST, "仅支持上传 JAR 格式的插件文件", null);
        }

        try {
            // 确保插件目录存在
            Files.createDirectories(pluginDir);

            // 保存文件
            Path targetPath = pluginDir.resolve(originalFilename);
            file.transferTo(targetPath.toFile());

            // 从文件名提取插件 ID（去除 .jar 后缀）
            String pluginId = originalFilename.replace(".jar", "");

            log.info("插件 JAR 已保存: {} (路径: {})", pluginId, targetPath);

            PluginView view = new PluginView(
                    pluginId,
                    "unknown",
                    pluginId,
                    "已上传，重启服务后生效",
                    PluginStateView.DISCOVERED,
                    "",
                    new PluginConfigurationView(pluginId, false, false, "missing", true, false)
            );
            return DataResult.success(view);
        } catch (IOException e) {
            log.error("插件上传失败: {}", originalFilename, e);
            return DataResult.of(StatusCodes.INTERNAL_ERROR, "插件文件保存失败: " + e.getMessage(), null);
        }
    }
}
