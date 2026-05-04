package com.nexusadmin.core.plugin.loader.impl;

import com.nexusadmin.core.Plugin;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.loader.PluginMetadata;
import com.nexusadmin.core.plugin.loader.PluginWrapper;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 默认插件加载器实现。
 * <p>通过 PluginSource 获取类路径，统一处理所有来源的插件加载。</p>
 */
public class DefaultPluginLoader implements PluginLoader {

    @Override
    public PluginWrapper load(PluginMetadata metadata) {
        try {
            URL[] urls = metadata.source().getClasspath();

            ClassLoader parent = getParentClassLoader();

            URLClassLoader classLoader = new URLClassLoader(urls, parent);

            Class<?> pluginClass = classLoader.loadClass(metadata.getMainClass());

            Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();

            return new PluginWrapper(metadata.descriptor(), plugin, classLoader, metadata.source());

        } catch (Exception e) {
            throw new PluginLoadException("加载插件失败: " + metadata.pluginId(), e);
        }
    }

    /**
     * 获取父类加载器。
     *
     * @return 父类加载器
     */
    protected ClassLoader getParentClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
