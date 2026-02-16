package com.nexusadmin.core.plugin.source;

import com.nexusadmin.core.plugin.loader.CandidatePlugin;
import com.nexusadmin.core.registry.Composable;

import java.util.List;

/**
 * 插件源抽象，负责从不同来源发现插件候选对象。
 * <p>支持本地目录、远程仓库、数据库等多种来源。</p>
 * <p>实现 {@link Composable} 以支持注册中心统一管理。</p>
 */
public interface PluginSource extends Composable {

    /**
     * 获取源类型标识。
     *
     * @return 源类型字符串，如 "local-directory", "classpath", "remote"
     */
    String sourceType();

    /**
     * 扫描并返回所有候选插件。
     *
     * @return 候选插件列表，不会返回 null
     */
    List<CandidatePlugin> scan();

    /**
     * 是否支持动态刷新（如远程源）。
     *
     * @return 如果支持动态刷新返回 true
     */
    default boolean supportsRefresh() {
        return false;
    }

    /**
     * 刷新插件源，重新扫描。
     * <p>默认实现直接调用 scan()。</p>
     *
     * @return 刷新后的候选插件列表
     */
    default List<CandidatePlugin> refresh() {
        return scan();
    }
}
