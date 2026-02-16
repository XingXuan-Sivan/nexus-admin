package com.nexusadmin.core.plugin.loader;

import com.nexusadmin.api.SpiRegistry;
import com.nexusadmin.api.Plugin;
import com.nexusadmin.api.PluginDescriptor;
import com.nexusadmin.api.exception.PluginLoadException;

/**
 * 类路径插件加载器，负责从当前运行环境的类路径中加载插件。
 * <p>适用于内置插件，直接使用当前线程上下文类加载器。</p>
 */
public class ClasspathPluginLoader implements PluginLoader {

    @Override
    public boolean supports(CandidatePlugin candidate) {
        // 只处理来自类路径的候选插件
        return candidate != null && candidate.sourceType() == SourceType.CLASSPATH;
    }

    @Override
    public LoadedPlugin load(CandidatePlugin candidate, SpiRegistry registry) {
        PluginDescriptor descriptor = candidate.descriptor();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Plugin plugin = null;
        try {
            if (descriptor.hasEntryPoint()) {
                Class<?> pluginClass = classLoader.loadClass(descriptor.mainClass());
                if (Plugin.class.isAssignableFrom(pluginClass)) {
                    plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
                }
            }
        } catch (Exception e) {
            throw new PluginLoadException("加载类路径插件失败: " + descriptor.id(), e);
        }

        return new LoadedPlugin(descriptor, plugin, classLoader, candidate.sourcePath());
    }
}
