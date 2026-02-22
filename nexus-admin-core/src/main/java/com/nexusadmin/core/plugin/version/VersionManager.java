package com.nexusadmin.core.plugin.version;

/**
 * 版本管理器接口，负责版本比较和兼容性检查。
 * <p>将版本相关逻辑从 PluginManager 中抽离，支持可替换的实现。</p>
 */
public interface VersionManager {

    /**
     * 检查核心版本是否满足插件的要求版本。
     *
     * @param coreVersion 当前核心版本号
     * @param required    插件要求的核心版本范围（如 ^1.0.0, >=1.0.0 <2.0.0）
     * @return 如果兼容返回 true，否则返回 false
     */
    boolean isCompatible(String coreVersion, String required);

    /**
     * 比较两个版本号的大小。
     *
     * @param version1 第一个版本号
     * @param version2 第二个版本号
     * @return 如果 version1 > version2 返回正数，相等返回 0，小于返回负数
     */
    int compare(String version1, String version2);

    /**
     * 验证版本号格式是否合法。
     *
     * @param version 版本号字符串
     * @return 如果格式合法返回 true
     */
    boolean isValid(String version);
}
