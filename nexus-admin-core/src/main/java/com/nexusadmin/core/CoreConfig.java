package com.nexusadmin.core;

import com.nexusadmin.core.plugin.RuntimeMode;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 核心运行时配置。
 * <p>封装平台级运行时配置项，如运行模式、插件数据目录等，
 * 避免 PluginManager 构造函数参数膨胀。</p>
 */
public class CoreConfig {
    private final RuntimeMode runtimeMode;
    private final Path pluginsDataRoot;

    private CoreConfig(RuntimeMode runtimeMode, Path pluginsDataRoot) {
        this.runtimeMode = Objects.requireNonNull(runtimeMode, "运行模式不能为空");
        this.pluginsDataRoot = Objects.requireNonNull(pluginsDataRoot, "插件数据目录不能为空");
    }

    public static CoreConfig of(RuntimeMode runtimeMode, Path pluginsDataRoot) {
        return new CoreConfig(runtimeMode, pluginsDataRoot);
    }

    public RuntimeMode runtimeMode() { return runtimeMode; }
    public Path pluginsDataRoot() { return pluginsDataRoot; }
}
