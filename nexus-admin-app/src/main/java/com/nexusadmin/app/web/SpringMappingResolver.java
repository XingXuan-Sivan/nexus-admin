package com.nexusadmin.app.web;

import com.nexusadmin.api.extension.web.AdminApi;
import com.nexusadmin.api.extension.web.MappingResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring MVC 映射解析器实现。
 * <p>
 * 解析控制器类上的 @RequestMapping 及相关注解（@GetMapping, @PostMapping 等），
 * 并支持插件路径前缀自动注入。
 */
public class SpringMappingResolver implements MappingResolver {

    private static final Logger log = LoggerFactory.getLogger(SpringMappingResolver.class);

    private static final String PLUGIN_PATH_PREFIX = "/api";
    private static final String ADMIN_PATH_PREFIX = "/admin";

    @Override
    public List<ResolvedMapping> resolve(Object controller, String pluginId) {
        List<ResolvedMapping> mappings = new ArrayList<>();
        Class<?> controllerClass = controller.getClass();

        boolean isAdminApi = AnnotatedElementUtils.hasAnnotation(controllerClass, AdminApi.class);
        String pathPrefix = buildPathPrefix(pluginId, isAdminApi);

        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controllerClass, RequestMapping.class);
        String[] classPaths = classMapping != null ? classMapping.path() : new String[0];

        for (Method method : controllerClass.getDeclaredMethods()) {
            RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (methodMapping == null) {
                continue;
            }

            RequestMappingInfo mappingInfo = createRequestMappingInfo(methodMapping, classPaths, pathPrefix);
            if (mappingInfo != null) {
                mappings.add(new ResolvedMapping(mappingInfo, controller, method));
                log.debug("解析到映射: {} -> {}#{}",
                        mappingInfo.getPatternValues(),
                        controllerClass.getSimpleName(),
                        method.getName());
            }
        }

        return mappings;
    }

    private String buildPathPrefix(String pluginId, boolean isAdminApi) {
        if (isAdminApi) {
            return ADMIN_PATH_PREFIX;
        }
        return PLUGIN_PATH_PREFIX.replace("{pluginId}", pluginId);
    }

    private RequestMappingInfo createRequestMappingInfo(
            RequestMapping methodMapping,
            String[] classPaths,
            String pathPrefix) {

        String[] methodPaths = methodMapping.path();
        List<String> combinedPaths = new ArrayList<>();

        if (methodPaths.length == 0) {
            methodPaths = new String[]{""};
        }

        if (classPaths.length == 0) {
            for (String path : methodPaths) {
                combinedPaths.add(normalizePath(pathPrefix + path));
            }
        } else {
            for (String classPath : classPaths) {
                for (String methodPath : methodPaths) {
                    combinedPaths.add(normalizePath(pathPrefix + classPath + methodPath));
                }
            }
        }

        return RequestMappingInfo
                .paths(combinedPaths.toArray(new String[0]))
                .methods(methodMapping.method())
                .params(methodMapping.params())
                .headers(methodMapping.headers())
                .consumes(methodMapping.consumes())
                .produces(methodMapping.produces())
                .build();
    }

    private String normalizePath(String path) {
        return path.replaceAll("/+", "/");
    }
}
