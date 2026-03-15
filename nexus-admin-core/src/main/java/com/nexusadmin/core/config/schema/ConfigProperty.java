package com.nexusadmin.core.config.schema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 配置属性定义，描述单个配置项的元数据。
 * <p>对应 schema.yml 中的 properties 项。</p>
 */
public final class ConfigProperty {

    /**
     * 属性键名。
     */
    private final String key;

    /**
     * 数据类型。
     */
    private final String type;

    /**
     * 显示标题。
     */
    private final String title;

    /**
     * 详细描述。
     */
    private final String description;

    /**
     * 默认值。
     */
    private final Object defaultValue;

    /**
     * 枚举值列表（仅当 type 为 enum 时有效）。
     */
    private final List<String> enumValues;

    /**
     * 数值最小值（仅当 type 为 number/integer 时有效）。
     */
    private final Number minimum;

    /**
     * 数值最大值（仅当 type 为 number/integer 时有效）。
     */
    private final Number maximum;

    /**
     * 是否必填。
     */
    private final boolean required;

    /**
     * 构造配置属性。
     *
     * @param key          属性键名
     * @param type         数据类型
     * @param title        显示标题
     * @param description  详细描述
     * @param defaultValue 默认值
     * @param enumValues   枚举值列表
     * @param minimum      数值最小值
     * @param maximum      数值最大值
     * @param required     是否必填
     */
    public ConfigProperty(String key,
                          String type,
                          String title,
                          String description,
                          Object defaultValue,
                          List<String> enumValues,
                          Number minimum,
                          Number maximum,
                          boolean required) {
        this.key = Objects.requireNonNull(key, "属性键名不能为空");
        this.type = Objects.requireNonNull(type, "属性类型不能为空");
        this.title = title != null ? title : key;
        this.description = description != null ? description : "";
        this.defaultValue = defaultValue;
        this.enumValues = enumValues != null ? Collections.unmodifiableList(enumValues) : null;
        this.minimum = minimum;
        this.maximum = maximum;
        this.required = required;
    }

    /**
     * 获取属性键名。
     *
     * @return 键名
     */
    public String key() {
        return key;
    }

    /**
     * 获取数据类型。
     *
     * @return 类型，如 string、integer、boolean、number、array、object
     */
    public String type() {
        return type;
    }

    /**
     * 获取显示标题。
     *
     * @return 标题
     */
    public String title() {
        return title;
    }

    /**
     * 获取详细描述。
     *
     * @return 描述
     */
    public String description() {
        return description;
    }

    /**
     * 获取默认值。
     *
     * @return 默认值，可能为 null
     */
    public Object defaultValue() {
        return defaultValue;
    }

    /**
     * 获取枚举值列表。
     *
     * @return 枚举值列表，可能为 null
     */
    public List<String> enumValues() {
        return enumValues;
    }

    /**
     * 获取数值最小值。
     *
     * @return 最小值，可能为 null
     */
    public Number minimum() {
        return minimum;
    }

    /**
     * 获取数值最大值。
     *
     * @return 最大值，可能为 null
     */
    public Number maximum() {
        return maximum;
    }

    /**
     * 检查是否必填。
     *
     * @return 如果必填返回 true
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 检查是否有枚举值限制。
     *
     * @return 如果有枚举值返回 true
     */
    public boolean hasEnumValues() {
        return enumValues != null && !enumValues.isEmpty();
    }

    /**
     * 检查是否有范围限制。
     *
     * @return 如果有最小值或最大值返回 true
     */
    public boolean hasRange() {
        return minimum != null || maximum != null;
    }

    @Override
    public String toString() {
        return String.format("ConfigProperty[key=%s, type=%s, title=%s]", key, type, title);
    }

    /**
     * 构建器模式创建 ConfigProperty。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ConfigProperty 构建器。
     */
    public static class Builder {
        private String key;
        private String type = "string";
        private String title;
        private String description;
        private Object defaultValue;
        private List<String> enumValues;
        private Number minimum;
        private Number maximum;
        private boolean required;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder enumValues(List<String> enumValues) {
            this.enumValues = enumValues;
            return this;
        }

        public Builder minimum(Number minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder maximum(Number maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public ConfigProperty build() {
            return new ConfigProperty(key, type, title, description, defaultValue,
                    enumValues, minimum, maximum, required);
        }
    }
}
