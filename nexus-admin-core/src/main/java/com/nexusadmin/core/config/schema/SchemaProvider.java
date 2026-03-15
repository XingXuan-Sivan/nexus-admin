package com.nexusadmin.core.config.schema;

import java.util.Optional;

/**
 * Schema 提供者 SPI 接口。
 * <p>支持从不同来源加载配置 Schema，包括插件 Schema 和平台 Schema。</p>
 */
public interface SchemaProvider {

    /**
     * 加载指定 schemaId 的 Schema。
     *
     * @param schemaId    配置域 ID，例如插件ID("order-plugin")或平台ID("platform")等
     * @param classLoader 对应的类加载器（插件/平台）
     * @return Schema，如果不存在或加载失败则返回空 Optional
     */
    Optional<ConfigSchema> load(String schemaId, ClassLoader classLoader);

    /**
     * 获取提供者名称。
     *
     * @return 提供者名称
     */
    String name();

    /**
     * 获取提供者优先级，数值越小优先级越高。
     *
     * @return 优先级数值
     */
    default int priority() {
        return 100;
    }
}
