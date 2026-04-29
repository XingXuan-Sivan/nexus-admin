package com.nexusadmin.app.config.properties;

/**
 * 平台基本信息配置。
 * <p>
 * 描述平台本身的元信息，如名称、版本、描述等。
 * 作为 {@link PlatformProperties} 的子配置组件。
 */
public class PlatformInfoProperties {

    /**
     * 平台名称。
     */
    private String name;

    /**
     * 平台版本号。
     */
    private String version;

    /**
     * 平台描述信息。
     */
    private String description;

    /**
     * 获取平台名称。
     *
     * @return 平台名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置平台名称。
     *
     * @param name 平台名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取平台版本号。
     *
     * @return 平台版本号
     */
    public String getVersion() {
        return version;
    }

    /**
     * 设置平台版本号。
     *
     * @param version 平台版本号
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 获取平台描述信息。
     *
     * @return 平台描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置平台描述信息。
     *
     * @param description 平台描述信息
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
