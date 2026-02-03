package com.nexusadmin.plugin.loader;

/**
 * 插件来源类型。
 */
public enum SourceType {
    /**
     * 类路径插件（内置在应用或库的类路径中）。
     */
    CLASSPATH,
    /**
     * 外部插件（从特定目录加载的 JAR 或目录）。
     */
    EXTERNAL,
    /**
     * 内置插件（由平台预置，可能具有更高优先级）。
     */
    BUILTIN,
    /**
     * 远程插件（从远程仓库下载）。
     */
    REMOTE
}
