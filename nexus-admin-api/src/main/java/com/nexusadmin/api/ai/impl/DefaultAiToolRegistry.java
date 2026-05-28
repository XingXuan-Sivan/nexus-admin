package com.nexusadmin.api.ai.impl;

import com.nexusadmin.api.ai.AiTool;
import com.nexusadmin.api.ai.AiToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具注册表默认实现，聚合 Spring Bean 发现与运行时动态注册。
 *
 * <p>Spring 管理的 AiTool Bean 通过构造函数注入，
 * 运行时动态注册的工具维护在 ConcurrentHashMap 中。
 * listAll() 自动合并两者并去重。</p>
 */
public class DefaultAiToolRegistry implements AiToolRegistry {

    /**
     * Spring 管理的 AiTool Bean 列表（由 Spring 自动注入）。
     */
    private final List<AiTool> springTools;

    /**
     * 运行时动态注册的工具。
     */
    private final ConcurrentHashMap<String, AiTool> runtimeTools = new ConcurrentHashMap<>();

    /**
     * 构造工具注册表。
     *
     * @param springTools Spring 管理的 AiTool Bean 列表，可为空
     */
    public DefaultAiToolRegistry(List<AiTool> springTools) {
        this.springTools = springTools != null ? List.copyOf(springTools) : List.of();
    }

    @Override
    public void register(AiTool tool) {
        runtimeTools.put(tool.getName(), tool);
    }

    @Override
    public void unregister(String toolName) {
        runtimeTools.remove(toolName);
    }

    @Override
    public List<AiTool> listAll() {
        List<AiTool> all = new ArrayList<>(springTools);
        for (AiTool tool : runtimeTools.values()) {
            // 运行时注册的同名工具覆盖 Spring Bean
            boolean exists = all.stream().anyMatch(s -> s.getName().equals(tool.getName()));
            if (!exists) {
                all.add(tool);
            }
        }
        return List.copyOf(all);
    }

    @Override
    public AiTool get(String toolName) {
        AiTool runtime = runtimeTools.get(toolName);
        if (runtime != null) {
            return runtime;
        }
        return springTools.stream()
                .filter(s -> s.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }
}
