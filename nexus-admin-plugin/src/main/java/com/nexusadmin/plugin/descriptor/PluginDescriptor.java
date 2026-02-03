package com.nexusadmin.plugin.descriptor;

import java.util.List;
import java.util.Map;

/**
 * 插件描述接口，定义插件的基础元数据信息。
 */
public interface PluginDescriptor {
    /**
     * 获取插件唯一标识。
     *
     * @return 插件 ID
     */
    String id();

    /**
     * 获取插件版本号。
     *
     * @return 版本号
     */
    String version();

    /**
     * 获取插件入口类全限定名。
     *
     * @return 入口类名，可能为空
     */
    String mainClass();

    /**
     * 获取插件提供的能力标识集合。
     *
     * @return 能力列表
     */
    List<String> provides();

    /**
     * 获取插件的其他扩展元数据。
     *
     * @return 元数据 Map
     */
    Map<String, Object> metadata();

    /**
     * 获取插件加载优先级，数值越大优先级越高。
     *
     * @return 优先级数值
     */
    default int priority() {
        return 0;
    }

    /**
     * 判断插件是否包含入口类。
     *
     * @return 如果 mainClass 不为空则返回 true
     */
    default boolean hasEntryPoint() {
        String main = mainClass();
        return main != null && !main.isBlank();
    }
}
