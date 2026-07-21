package com.nexusadmin.core.plugin.discovery.impl;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.nexusadmin.core.plugin.discovery.PluginContributes;
import com.nexusadmin.core.plugin.discovery.PluginDescriptor;
import com.nexusadmin.core.exception.DescriptorParseException;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorFinder;
import com.nexusadmin.core.plugin.discovery.PluginDescriptorParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.nexusadmin.core.plugin.discovery.PluginDescriptorKeys.*;

/**
 * 基于 JSON 的插件描述解析器。
 * <p>支持从目录和 JAR 文件解析 META-INF/plugin.json。</p>
 */
public class JsonPluginDescriptorParser implements PluginDescriptorParser {

    /**
     * 标准字段集合，用于区分标准字段和元数据字段。
     */
    private static final Set<String> STANDARD_KEY_SET = new HashSet<>();

    static {
        for (String key : STANDARD_KEYS) {
            STANDARD_KEY_SET.add(key);
        }
    }

    private final List<PluginDescriptorFinder> finders;

    public JsonPluginDescriptorParser(List<PluginDescriptorFinder> finders) {
        this.finders = List.copyOf(finders != null ? finders : List.of());
    }

    @Override
    public boolean supports(Path pluginPath) {
        if (Files.isDirectory(pluginPath)) {
            return finders.stream().anyMatch(f -> f.find(pluginPath).isPresent());
        }
        return pluginPath.toString().endsWith(".jar");
    }

    @Override
    public PluginDescriptor parse(Path pluginPath) {
        if (Files.isDirectory(pluginPath)) {
            return parseFromDirectory(pluginPath);
        }
        return parseFromJar(pluginPath);
    }

    private PluginDescriptor parseFromDirectory(Path dir) {
        Optional<Path> descriptorPath = finders.stream()
                .sorted((f1, f2) -> Integer.compare(f2.priority(), f1.priority()))
                .map(f -> f.find(dir))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (descriptorPath.isEmpty()) {
            throw new DescriptorParseException("在目录中未找到插件描述文件: " + dir);
        }

        try (InputStream is = Files.newInputStream(descriptorPath.get())) {
            return parseStream(is);
        } catch (IOException e) {
            throw new DescriptorParseException("读取描述文件失败: " + descriptorPath.get(), e);
        }
    }

    private PluginDescriptor parseFromJar(Path jarPath) {
        if (!jarPath.toString().endsWith(".jar")) {
            throw new DescriptorParseException("不支持的文件类型: " + jarPath);
        }
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(DESCRIPTOR_PATH);
            if (entry == null) {
                throw new DescriptorParseException("JAR 中未找到 " + DESCRIPTOR_PATH + ": " + jarPath);
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                return parseStream(is);
            }
        } catch (IOException e) {
            throw new DescriptorParseException("读取 JAR 描述文件失败: " + jarPath, e);
        }
    }

    private PluginDescriptor parseStream(InputStream source) {
        if (source == null) {
            throw new DescriptorParseException("输入流为空");
        }
        try (InputStreamReader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            JsonObject json = Json.parse(reader).asObject();

            // 必填字段校验
            if (!json.names().contains(KEY_ID) || !json.names().contains(KEY_VERSION)) {
                throw new DescriptorParseException("plugin.json 缺失必填字段: " + KEY_ID + " 或 " + KEY_VERSION);
            }

            // 必填字段
            String id = json.getString(KEY_ID, "").trim();
            String version = json.getString(KEY_VERSION, "").trim();

            // 可选字段
            String name = json.getString(KEY_NAME, "").trim();
            String description = json.getString(KEY_DESCRIPTION, "").trim();
            String author = json.getString(KEY_AUTHOR, "").trim();
            String mainClass = json.getString(KEY_MAIN_CLASS, "").trim();
            String coreVersion = json.getString(KEY_CORE_VERSION, "").trim();

            // 复杂可选字段
            Map<String, String> dependencies = parseDependencies(json.get(KEY_DEPENDENCIES));
            Map<String, Object> requires = parseRequires(json.get(KEY_REQUIRES));

            // 元数据（非标准字段）
            Map<String, Object> metadata = extractMetadata(json);

            // 贡献声明
            PluginContributes contributes = parseContributes(json.get(KEY_CONTRIBUTES));

            return new PluginDescriptor(
                    id, version, name, description, author,
                    mainClass, coreVersion, dependencies, requires, metadata, contributes
            );
        } catch (com.eclipsesource.json.ParseException e) {
            throw new DescriptorParseException("JSON 语法错误: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new DescriptorParseException("读取描述文件时发生 I/O 错误", e);
        } catch (IllegalStateException | NullPointerException e) {
            throw new DescriptorParseException("plugin.json 字段类型错误", e);
        }
    }

    /**
     * 解析 dependencies 字段值。
     *
     * @param value JSON 值
     * @return 依赖插件Map，key为插件ID，value为版本范围
     */
    private Map<String, String> parseDependencies(JsonValue value) {
        Map<String, String> deps = new HashMap<>();
        if (value == null || value.isNull()) {
            return deps;
        }
        if (value.isObject()) {
            JsonObject obj = value.asObject();
            for (String key : obj.names()) {
                JsonValue val = obj.get(key);
                if (val.isString()) {
                    deps.put(key, val.asString());
                }
            }
        }
        return deps;
    }

    /**
     * 解析 requires 字段值。
     *
     * @param value JSON 值
     * @return 环境要求Map
     */
    private Map<String, Object> parseRequires(JsonValue value) {
        Map<String, Object> reqs = new HashMap<>();
        if (value == null || value.isNull()) {
            return reqs;
        }
        if (value.isObject()) {
            JsonObject obj = value.asObject();
            for (String key : obj.names()) {
                reqs.put(key, toJsonValue(obj.get(key)));
            }
        }
        return reqs;
    }

    /**
     * 提取非标准字段作为元数据。
     *
     * @param json JSON 对象
     * @return 元数据 Map
     */
    private Map<String, Object> extractMetadata(JsonObject json) {
        Map<String, Object> meta = new HashMap<>();
        for (String key : json.names()) {
            if (!STANDARD_KEY_SET.contains(key)) {
                meta.put(key, toJsonValue(json.get(key)));
            }
        }
        return meta;
    }

    /**
     * 解析 contributes 字段值。
     *
     * @param value JSON 值
     * @return 插件贡献声明，不为 null
     */
    private PluginContributes parseContributes(JsonValue value) {
        if (value == null || value.isNull() || !value.isObject()) {
            return PluginContributes.EMPTY;
        }

        JsonObject obj = value.asObject();
        List<PluginContributes.MenuContribution> menus = parseMenus(obj.get(KEY_CONTRIBUTES_MENUS));
        List<PluginContributes.RouteContribution> routes = parseRoutes(obj.get(KEY_CONTRIBUTES_ROUTES));
        List<PluginContributes.MountPointContribution> mountPoints = parseMountPoints(obj.get(KEY_CONTRIBUTES_MOUNT_POINTS));
        List<PluginContributes.PermissionContribution> permissions = parsePermissions(obj.get(KEY_CONTRIBUTES_PERMISSIONS));

        return new PluginContributes(menus, routes, mountPoints, permissions);
    }

    /**
     * 解析 menus 贡献列表。
     *
     * @param value JSON 值
     * @return 菜单贡献列表
     */
    private List<PluginContributes.MenuContribution> parseMenus(JsonValue value) {
        List<PluginContributes.MenuContribution> menus = new ArrayList<>();
        if (value == null || value.isNull() || !value.isArray()) {
            return menus;
        }
        for (JsonValue item : value.asArray()) {
            if (!item.isObject()) continue;
            JsonObject obj = item.asObject();
            menus.add(new PluginContributes.MenuContribution(
                    obj.getString("id", ""),
                    obj.getString("label", ""),
                    obj.getString("icon", ""),
                    obj.getString("parentId", ""),
                    obj.getInt("order", 0),
                    obj.getString("route", ""),
                    parseStringList(obj.get("permissions"))
            ));
        }
        return menus;
    }

    /**
     * 解析 routes 贡献列表。
     *
     * @param value JSON 值
     * @return 路由贡献列表
     */
    private List<PluginContributes.RouteContribution> parseRoutes(JsonValue value) {
        List<PluginContributes.RouteContribution> routes = new ArrayList<>();
        if (value == null || value.isNull() || !value.isArray()) {
            return routes;
        }
        for (JsonValue item : value.asArray()) {
            if (!item.isObject()) continue;
            JsonObject obj = item.asObject();
            List<String> perms = parseStringList(obj.get("permissions"));
            routes.add(new PluginContributes.RouteContribution(
                    obj.getString("path", ""),
                    obj.getString("component", ""),
                    obj.getString("title", ""),
                    obj.getString("icon", ""),
                    perms
            ));
        }
        return routes;
    }

    /**
     * 解析 mountPoints 贡献列表。
     *
     * @param value JSON 值
     * @return 挂载点贡献列表
     */
    @SuppressWarnings("unchecked")
    private List<PluginContributes.MountPointContribution> parseMountPoints(JsonValue value) {
        List<PluginContributes.MountPointContribution> mountPoints = new ArrayList<>();
        if (value == null || value.isNull() || !value.isArray()) {
            return mountPoints;
        }
        for (JsonValue item : value.asArray()) {
            if (!item.isObject()) continue;
            JsonObject obj = item.asObject();
            Map<String, Object> props = new HashMap<>();
            JsonValue propsValue = obj.get("props");
            if (propsValue != null && propsValue.isObject()) {
                for (String k : propsValue.asObject().names()) {
                    props.put(k, toJsonValue(propsValue.asObject().get(k)));
                }
            }
            mountPoints.add(new PluginContributes.MountPointContribution(
                    obj.getString("target", ""),
                    obj.getString("component", ""),
                    obj.getInt("order", 0),
                    props
            ));
        }
        return mountPoints;
    }

    /**
     * 解析 permissions 贡献列表。
     *
     * @param value JSON 值
     * @return 权限贡献列表
     */
    private List<PluginContributes.PermissionContribution> parsePermissions(JsonValue value) {
        List<PluginContributes.PermissionContribution> permissions = new ArrayList<>();
        if (value == null || value.isNull() || !value.isArray()) {
            return permissions;
        }
        for (JsonValue item : value.asArray()) {
            if (!item.isObject()) continue;
            JsonObject obj = item.asObject();
            permissions.add(new PluginContributes.PermissionContribution(
                    obj.getString("id", ""),
                    obj.getString("label", ""),
                    obj.getString("description", "")
            ));
        }
        return permissions;
    }

    /**
     * 解析字符串列表。
     *
     * @param value JSON 值
     * @return 字符串列表
     */
    private List<String> parseStringList(JsonValue value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isNull() || !value.isArray()) {
            return result;
        }
        for (JsonValue item : value.asArray()) {
            if (item.isString()) {
                result.add(item.asString());
            }
        }
        return result;
    }

    /**
     * 将 JsonValue 转换为 Java 对象。
     *
     * @param value JSON 值
     * @return 对应的 Java 对象
     */
    private Object toJsonValue(JsonValue value) {
        if (value == null || value.isNull()) return null;
        if (value.isString()) return value.asString();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) return value.asDouble();
        if (value.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonValue v : value.asArray()) {
                list.add(toJsonValue(v));
            }
            return list;
        }
        if (value.isObject()) {
            Map<String, Object> map = new HashMap<>();
            for (String k : value.asObject().names()) {
                map.put(k, toJsonValue(value.asObject().get(k)));
            }
            return map;
        }
        return value.toString();
    }
}
