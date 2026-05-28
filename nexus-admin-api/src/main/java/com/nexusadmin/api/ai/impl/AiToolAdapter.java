package com.nexusadmin.api.ai.impl;

import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.ai.AiTool;
import com.alibaba.fastjson2.JSON;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 LangChain4j @Tool 注解方法适配为平台 AiTool 的适配器。
 *
 * <p>当扫描到插件中带有 {@code @Tool} 注解的方法时，
 * 通过此适配器包装为 AiTool，即可注册到 AiToolRegistry，
 * 进而自动暴露为 MCP Tool。</p>
 *
 * <p>适配器提取 @Tool 注解的 name/description，
 * 自动生成参数 JSON Schema，通过反射调用目标方法。</p>
 */
public class AiToolAdapter implements AiTool {

    private static final Logger log = LoggerFactory.getLogger(AiToolAdapter.class);

    private final String name;
    private final String description;
    private final String inputTypeSchema;
    private final Object target;
    private final Method method;

    /**
     * 从 @Tool 注解方法构造适配器。
     *
     * @param target 目标对象实例
     * @param method 被 @Tool 注解的方法
     */
    public AiToolAdapter(Object target, Method method) {
        this.target = target;
        this.method = method;
        Tool tool = method.getAnnotation(Tool.class);
        String[] toolNames = extractAnnotationValue(tool.name());
        this.name = (toolNames.length > 0 && !toolNames[0].isBlank())
                ? toolNames[0] : method.getName();
        String[] toolValues = extractAnnotationValue(tool.value());
        this.description = (toolValues.length > 0 && !toolValues[0].isBlank())
                ? toolValues[0] : method.getName();
        this.inputTypeSchema = buildInputSchema(method);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getInputTypeSchema() {
        return inputTypeSchema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, InvocationContext context) {
        try {
            Object[] args = resolveArgs(arguments);
            Object result = method.invoke(target, args);
            String resultStr = (result instanceof String)
                    ? (String) result
                    : JSON.toJSONString(result);
            return new ToolResult(true, "执行成功", resultStr);
        } catch (InvocationTargetException e) {
            log.error("@Tool 方法执行异常: {}.{}", target.getClass().getSimpleName(), method.getName(), e.getCause());
            return new ToolResult(false, "执行失败: " + e.getCause().getMessage(), null);
        } catch (Exception e) {
            log.error("@Tool 方法调用失败: {}.{}", target.getClass().getSimpleName(), method.getName(), e);
            return new ToolResult(false, "调用失败: " + e.getMessage(), null);
        }
    }

    /**
     * 将参数 Map 解析为方法实参数组。
     */
    private Object[] resolveArgs(Map<String, Object> arguments) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String paramName = params[i].getName();
            Object value = arguments.get(paramName);
            if (value != null) {
                args[i] = JSON.toJavaObject(JSON.toJSON(value), params[i].getType());
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    /**
     * 根据方法参数签名构建简易 JSON Schema。
     */
    private static String buildInputSchema(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Parameter param : method.getParameters()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            String type = mapJavaTypeToJsonType(param.getType());
            prop.put("type", type);
            if (param.getType() == String.class && param.getName().equals("arg0")) {
                prop.put("description", param.getName());
            }
            properties.put(param.getName(), prop);
        }
        schema.put("properties", properties);
        return JSON.toJSONString(schema);
    }

    /**
     * 将 Java 基础类型映射为 JSON Schema type。
     */
    private static String mapJavaTypeToJsonType(Class<?> type) {
        if (type == String.class || type == char.class || type == Character.class) {
            return "string";
        }
        if (type == int.class || type == long.class || type == Integer.class || type == Long.class) {
            return "integer";
        }
        if (type == double.class || type == float.class || type == Double.class || type == Float.class) {
            return "number";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        return "object";
    }

    /**
     * 从注解值中提取 String[]（兼容 String 与 String[] 两种返回类型）。
     */
    private static String[] extractAnnotationValue(Object value) {
        if (value instanceof String[] arr) {
            return arr;
        }
        if (value instanceof String s && !s.isBlank()) {
            return new String[]{s};
        }
        return new String[0];
    }
}
