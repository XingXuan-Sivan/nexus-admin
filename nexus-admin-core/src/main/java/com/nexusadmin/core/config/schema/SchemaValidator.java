package com.nexusadmin.core.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Schema 验证器 SPI 接口。
 * <p>支持不同的 Schema 验证实现，与具体验证库解耦。</p>
 */
public interface SchemaValidator {

    /**
     * 验证数据是否符合 Schema 定义。
     *
     * @param schemaNode Schema 定义节点
     * @param dataNode   待验证的数据节点
     * @return 验证错误列表，为空表示验证通过
     */
    List<ValidationMessage> validate(JsonNode schemaNode, JsonNode dataNode);

    /**
     * 验证 JSON 字符串数据是否符合 Schema 字符串定义。
     *
     * @param schemaJson Schema 定义字符串
     * @param dataJson   待验证的数据字符串
     * @return 验证错误列表，为空表示验证通过
     */
    default List<ValidationMessage> validate(String schemaJson, String dataJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode schemaNode = mapper.readTree(schemaJson);
            JsonNode dataNode = mapper.readTree(dataJson);
            return validate(schemaNode, dataNode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Schema 或配置 JSON 语法无效");
        }
    }

    /**
     * 获取验证器名称。
     *
     * @return 验证器名称
     */
    String name();

    /**
     * 获取验证器优先级，数值越小优先级越高。
     *
     * @return 优先级数值
     */
    default int priority() {
        return 100;
    }
}
