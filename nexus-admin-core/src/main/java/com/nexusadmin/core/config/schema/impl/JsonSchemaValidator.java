package com.nexusadmin.core.config.schema.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.nexusadmin.core.config.schema.SchemaValidator;
import com.nexusadmin.core.config.schema.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JSON Schema 验证器，基于 networknt/json-schema-validator 实现。
 * <p>支持 JSON Schema Draft 2020-12，自动忽略 x-ui-options、x-ui-group 等自定义关键字。</p>
 * <p>不依赖 Spring，保持 core 模块的零框架依赖原则。</p>
 */
public final class JsonSchemaValidator implements SchemaValidator {

    /**
     * 自定义关键字前缀，以 x- 开头的关键字不报验证错误。
     */
    private static final String CUSTOM_KEYWORD_PREFIX = "x-";

    /**
     * Jackson ObjectMapper 实例。
     */
    private final ObjectMapper objectMapper;

    /**
     * JSON Schema 工厂，使用 Draft 2020-12 规范。
     */
    private final JsonSchemaFactory schemaFactory;

    /**
     * 构造 JSON Schema 验证器。
     */
    public JsonSchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * 使用指定 ObjectMapper 构造 JSON Schema 验证器。
     *
     * @param objectMapper Jackson ObjectMapper 实例
     */
    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @Override
    public List<ValidationMessage> validate(JsonNode schemaNode, JsonNode dataNode) {
        if (schemaNode == null || dataNode == null) {
            return List.of();
        }

        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setTypeLoose(true);

        JsonSchema schema = schemaFactory.getSchema(schemaNode, config);
        Set<com.networknt.schema.ValidationMessage> networkntMessages = schema.validate(dataNode);

        // 过滤自定义关键字产生的错误并转换为自定义 ValidationMessage
        List<ValidationMessage> result = new ArrayList<>();
        for (com.networknt.schema.ValidationMessage msg : networkntMessages) {
            if (!isCustomKeywordError(msg)) {
                result.add(convertMessage(msg));
            }
        }

        return result;
    }

    @Override
    public List<ValidationMessage> validate(String schemaJson, String dataJson) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonNode dataNode = objectMapper.readTree(dataJson);
            return validate(schemaNode, dataNode);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return "json-schema";
    }

    @Override
    public int priority() {
        return 50;
    }

    /**
     * 将 networknt 的 ValidationMessage 转换为自定义 ValidationMessage。
     *
     * @param msg networknt 验证消息
     * @return 自定义验证消息
     */
    private ValidationMessage convertMessage(com.networknt.schema.ValidationMessage msg) {
        String keyword = msg.getType();
        String path = msg.getInstanceLocation() != null ? msg.getInstanceLocation().toString() : null;
        String message = msg.getMessage();
        return new ValidationMessage(keyword, path, message);
    }

    /**
     * 判断验证消息是否由自定义关键字（x- 前缀）引起。
     *
     * @param message networknt 验证消息
     * @return 如果是自定义关键字相关错误返回 true
     */
    private boolean isCustomKeywordError(com.networknt.schema.ValidationMessage message) {
        // 检查验证类型（即关键字名称）是否以 x- 开头
        String type = message.getType();
        if (type != null && type.startsWith(CUSTOM_KEYWORD_PREFIX)) {
            return true;
        }

        // 检查实例路径的最后一个片段是否以 x- 开头
        var instanceLocation = message.getInstanceLocation();
        if (instanceLocation != null) {
            String path = instanceLocation.toString();
            if (path != null && !path.isEmpty()) {
                String lastSegment = path.contains("/")
                        ? path.substring(path.lastIndexOf('/') + 1)
                        : path;
                if (lastSegment.startsWith(CUSTOM_KEYWORD_PREFIX)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取 ObjectMapper 实例。
     *
     * @return ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
