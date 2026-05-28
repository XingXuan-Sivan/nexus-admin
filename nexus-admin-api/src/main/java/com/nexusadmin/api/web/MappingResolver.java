package com.nexusadmin.api.web;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 映射解析器，用于从控制器类中解析出请求映射信息。
 * <p>
 * 不同 Web 框架可以提供各自的解析实现。
 */
public interface MappingResolver {

    /**
     * 解析控制器中的所有请求映射。
     *
     * @param controller 控制器实例
     * @param pluginId   插件唯一标识
     * @return 解析后的映射列表
     */
    List<ResolvedMapping> resolve(Object controller, String pluginId);

    /**
     * 解析后的映射信息。
     * <p>
     * mappingInfo 的具体类型由具体实现决定。
     *
     * @param mappingInfo 映射信息对象
     * @param handler     处理器对象
     * @param method      处理方法
     */
    record ResolvedMapping(
            Object mappingInfo,
            Object handler,
            Method method
    ) {
    }
}
