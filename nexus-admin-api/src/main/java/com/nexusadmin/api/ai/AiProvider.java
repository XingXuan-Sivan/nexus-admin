package com.nexusadmin.api.ai;

import com.nexusadmin.core.extension.ExtensionPoint;

/**
 * AI 服务提供者扩展点，平台级 AI 对话能力抽象。
 *
 * <p>作为平台 ExtensionPoint，支持多实现竞争和运行时热替换。
 * 插件可基于 LangChain4j、OpenAI SDK 等任意底层实现，
 * 只需通过此接口暴露统一对话能力。</p>
 *
 * <p><strong>插件实现示例：</strong>
 * <pre>{@code
 * public class OllamaAiProvider implements AiProvider {
 *     private final ChatLanguageModel model;
 *
 *     // 委托 generate 给 LangChain4j ChatLanguageModel
 *     public String generate(String prompt) { return model.generate(prompt); }
 *     public AiProviderInfo getInfo() { return new AiProviderInfo("ollama", "qwen2.5:7b", "1.0.0"); }
 * }}</pre>
 *
 * <p>平台通过 ExtensionConsumer&lt;AiProvider&gt; 动态获取最高优先级实现，
 * 无需显式依赖特定 AI 框架。</p>
 */
public interface AiProvider extends ExtensionPoint {

    /**
     * 发送对话提示词并获取 AI 回复。
     *
     * @param prompt 用户提示词
     * @return AI 生成的回复文本
     */
    String generate(String prompt);

    /**
     * 获取此 AI 提供者的元信息。
     *
     * @return AI 提供者元信息
     */
    AiProviderInfo getInfo();

    /**
     * AI 提供者元信息。
     *
     * @param name    提供者名称
     * @param model   模型名称
     * @param version 版本号
     */
    record AiProviderInfo(String name, String model, String version) {
    }
}
