package com.nexusadmin.plugin.loader;

import com.nexusadmin.core.spi.SpiRegistry;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public interface PluginLoader {
    /**
     * 判断当前加载器是否支持加载给定路径。
     *
     * @param path 插件路径
     * @return 如果支持则返回 true
     */
    boolean canLoad(Path path);

    /**
     * 发现阶段：从指定路径探测并返回候选插件信息（不执行安装）。
     *
     * @param path     插件所在路径
     * @param registry SPI 注册中心
     * @return 候选插件对象
     * @throws UnsupportedOperationException 如果加载器不支持通过路径发现插件
     */
    default PluginCandidate discover(Path path, SpiRegistry registry) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * 发现阶段：自动发现并返回所有可用候选插件列表（不执行安装）。
     *
     * @param registry SPI 注册中心
     * @return 候选插件列表
     */
    default List<PluginCandidate> discoverAll(SpiRegistry registry) {
        return Collections.emptyList();
    }
    
    /**
     * 安装阶段：执行真正的插件加载动作，返回已加载插件对象。
     *
     * @param candidate 候选插件信息
     * @param registry  SPI 注册中心
     * @return 已加载插件对象
     */
    LoadedPlugin load(PluginCandidate candidate, SpiRegistry registry);
    
    /**
     * 是否支持插件物理卸载（文件删除）。
     * <p>classpath 等只读插件应返回 false。</p>
     *
     * @return 如果支持物理卸载则返回 true
     */
    default boolean supportsRemove() {
        return false;
    }

    /**
     * 执行插件的物理卸载（删除插件文件或目录）。
     *
     * @param plugin 已加载插件
     */
    default void remove(LoadedPlugin plugin) {
        throw new UnsupportedOperationException("不支持物理卸载");
    }
}
