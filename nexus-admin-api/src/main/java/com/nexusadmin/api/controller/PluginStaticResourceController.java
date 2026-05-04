package com.nexusadmin.api.controller;

import com.nexusadmin.core.PluginManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * 插件静态资源托管控制器，从插件的 ClassLoader 中加载 static/ 目录下的资源文件。
 *
 * <p>访问路径：{@code GET /plugins/{pluginId}/assets/**}</p>
 *
 * <p><strong>安全措施：</strong></p>
 * <ul>
 *   <li>只允许访问 static/ 目录下的资源</li>
 *   <li>防止路径遍历攻击（禁止 .. 和绝对路径）</li>
 *   <li>仅支持常见 MIME 类型</li>
 * </ul>
 */
@RestController
@RequestMapping("/plugins")
public class PluginStaticResourceController {

    private static final Logger log = LoggerFactory.getLogger(PluginStaticResourceController.class);

    private static final String STATIC_PREFIX = "static/";
    private static final int MAX_RESOURCE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ASSETS_PREFIX = "/plugins/";

    /**
     * 常见 MIME 类型映射。
     */
    private static final Map<String, MediaType> MIME_MAP = Map.ofEntries(
            Map.entry("html", MediaType.TEXT_HTML),
            Map.entry("htm", MediaType.TEXT_HTML),
            Map.entry("css", MediaType.valueOf("text/css")),
            Map.entry("js", MediaType.valueOf("application/javascript")),
            Map.entry("json", MediaType.APPLICATION_JSON),
            Map.entry("png", MediaType.IMAGE_PNG),
            Map.entry("jpg", MediaType.IMAGE_JPEG),
            Map.entry("jpeg", MediaType.IMAGE_JPEG),
            Map.entry("gif", MediaType.IMAGE_GIF),
            Map.entry("svg", MediaType.valueOf("image/svg+xml")),
            Map.entry("ico", MediaType.valueOf("image/x-icon")),
            Map.entry("woff", MediaType.valueOf("font/woff")),
            Map.entry("woff2", MediaType.valueOf("font/woff2")),
            Map.entry("ttf", MediaType.valueOf("font/ttf")),
            Map.entry("eot", MediaType.valueOf("application/vnd.ms-fontobject")),
            Map.entry("xml", MediaType.APPLICATION_XML),
            Map.entry("txt", MediaType.TEXT_PLAIN),
            Map.entry("webp", MediaType.valueOf("image/webp"))
    );

    private final PluginManager pluginManager;

    /**
     * 构造插件静态资源控制器。
     *
     * @param pluginManager 插件管理器
     */
    public PluginStaticResourceController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * 加载插件静态资源。
     *
     * @param pluginId 插件标识
     * @param request  HTTP 请求，用于提取子路径
     * @return 资源内容响应
     */
    @GetMapping("/{pluginId}/assets/**")
    public ResponseEntity<byte[]> getAsset(@PathVariable String pluginId,
                                           HttpServletRequest request) {
        // 从请求 URI 中提取 assets/ 之后的子路径
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathWithoutContext = requestUri.substring(contextPath.length());
        String assetsMarker = "/plugins/" + pluginId + "/assets/";
        int assetsIndex = pathWithoutContext.indexOf(assetsMarker);
        if (assetsIndex < 0) {
            return ResponseEntity.notFound().build();
        }
        String resourcePath = pathWithoutContext.substring(assetsIndex + assetsMarker.length());

        // 安全检查：校验路径不含遍历攻击字符
        if (!isSafePath(resourcePath)) {
            log.warn("插件资源路径不安全，拒绝访问: 插件={}, 路径={}", pluginId, resourcePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 检查插件是否已激活
        if (!pluginManager.isActive(pluginId)) {
            log.debug("插件未激活，拒绝访问资源: 插件={}", pluginId);
            return ResponseEntity.notFound().build();
        }

        var wrapper = pluginManager.get(pluginId);
        if (wrapper == null) {
            return ResponseEntity.notFound().build();
        }

        // 构造资源路径：只允许 static/ 目录下
        String fullResourcePath = STATIC_PREFIX + resourcePath;

        ClassLoader classLoader = wrapper.classLoader();
        try (InputStream is = classLoader.getResourceAsStream(fullResourcePath)) {
            if (is == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] content = is.readNBytes(MAX_RESOURCE_SIZE);
            MediaType contentType = resolveContentType(resourcePath);

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(content);
        } catch (Exception e) {
            log.error("读取插件资源失败: 插件={}, 路径={}", pluginId, resourcePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 校验资源路径安全性，防止路径遍历攻击。
     *
     * @param path 资源路径
     * @return 安全返回 true
     */
    private boolean isSafePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.contains("..")) {
            return false;
        }
        if (path.startsWith("/")) {
            return false;
        }
        if (path.contains("\\")) {
            return false;
        }
        return true;
    }

    /**
     * 根据文件扩展名解析 MIME 类型。
     *
     * @param path 文件路径
     * @return MIME 类型，未知类型默认为 application/octet-stream
     */
    private MediaType resolveContentType(String path) {
        String fileName = Path.of(path).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String ext = fileName.substring(dotIndex + 1).toLowerCase();
        return MIME_MAP.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM);
    }
}
