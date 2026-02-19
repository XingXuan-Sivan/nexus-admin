package com.nexusadmin.core.extension;

import java.util.Objects;

/**
 * 扩展元数据。
 * <p>用于描述扩展实现的基本信息，在索引存储和发现过程中传递。</p>
 *
 * @author NexusAdmin
 * @since 1.0.0
 */
public final class ExtensionMetadata {

    private final String implementationClassName;
    private final String pluginId;

    /**
     * 构造扩展元数据。
     *
     * @param implementationClassName 扩展实现类全限定名
     * @param pluginId                所属插件ID
     */
    public ExtensionMetadata(String implementationClassName, String pluginId) {
        this.implementationClassName = Objects.requireNonNull(
                implementationClassName, "实现类名不能为空");
        this.pluginId = pluginId != null ? pluginId : "";
    }

    /**
     * 获取扩展实现类全限定名。
     *
     * @return 实现类全限定名
     */
    public String getImplementationClassName() {
        return implementationClassName;
    }

    /**
     * 获取所属插件ID。
     *
     * @return 插件ID，可能为空字符串
     */
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionMetadata that = (ExtensionMetadata) o;
        return Objects.equals(implementationClassName, that.implementationClassName) &&
                Objects.equals(pluginId, that.pluginId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(implementationClassName, pluginId);
    }

    @Override
    public String toString() {
        return "ExtensionMetadata{" +
                "implementationClassName='" + implementationClassName + '\'' +
                ", pluginId='" + pluginId + '\'' +
                '}';
    }
}
