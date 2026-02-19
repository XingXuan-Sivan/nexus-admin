package com.nexusadmin.core.plugin.descriptor.parser;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.nexusadmin.core.plugin.PluginDescriptor;
import com.nexusadmin.core.exception.DescriptorParseException;
import com.nexusadmin.core.plugin.descriptor.PluginDescriptorParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nexusadmin.core.plugin.descriptor.PluginDescriptorKeys.*;

/**
 * 基于 JSON 的插件描述解析器。
 * <p>专注从 InputStream 解析 META-INF/plugin.json。</p>
 */
public class JsonPluginDescriptorParser implements PluginDescriptorParser<InputStream> {

    /**
     * 标准字段集合，用于区分标准字段和元数据字段。
     */
    private static final Set<String> STANDARD_KEY_SET = new HashSet<>();

    static {
        for (String key : STANDARD_KEYS) {
            STANDARD_KEY_SET.add(key);
        }
    }

    /**
     * 从输入流解析插件描述。
     *
     * @param source JSON 输入流
     * @return 解析后的插件描述对象
     * @throws DescriptorParseException 当解析失败时抛出
     */
    @Override
    public PluginDescriptor parse(InputStream source) {
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

            return new PluginDescriptor(
                    id, version, name, description, author,
                    mainClass, coreVersion, dependencies, requires, metadata
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
     * 将 JsonValue 转换为 Java 对象。
     *
     * @param value JSON 值
     * @return 对应的 Java 对象
     */
    private Object toJsonValue(JsonValue value) {
        if (value == null || value.isNull()) return null;
        if (value.isString()) return value.asString();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) return value.asDouble(); // or asInt() if needed
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
