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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        config.setTypeLoose(false);

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
            throw new IllegalArgumentException("Schema 或配置 JSON 语法无效");
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
        // networknt messages may interpolate the rejected instance value. API-facing
        // validation messages must be derived only from the schema keyword and safe params.
        String message = "配置值不符合 Schema 约束: "
                + (keyword == null || keyword.isBlank() ? "invalid" : keyword);
        return new ValidationMessage(keyword, path, message, safeParams(msg));
    }

    /**
     * 只映射 Schema 阈值与缺失属性等安全参数，绝不透传 instanceNode 或完整 details。
     */
    private Map<String, Object> safeParams(com.networknt.schema.ValidationMessage message) {
        Object[] arguments = message.getArguments();
        if (arguments == null || arguments.length == 0 || message.getType() == null) {
            return Map.of();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        switch (message.getType()) {
            case "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum",
                    "minLength", "maxLength", "minItems", "maxItems",
                    "minProperties", "maxProperties" -> params.put("limit", arguments[0]);
            case "multipleOf" -> params.put("multipleOf", arguments[0]);
            case "required" -> params.put("property", arguments[0]);
            case "type" -> params.put("expected", arguments[0]);
            default -> {
                return Map.of();
            }
        }
        return Map.copyOf(params);
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
