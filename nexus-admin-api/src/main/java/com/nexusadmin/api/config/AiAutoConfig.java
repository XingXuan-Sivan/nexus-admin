package com.nexusadmin.api.config;

import com.nexusadmin.api.extension.ai.AiProvider;
import com.nexusadmin.api.extension.ai.AiTool;
import com.nexusadmin.api.extension.ai.AiToolRegistry;
import com.nexusadmin.api.extension.ai.impl.DefaultAiToolRegistry;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionConsumer;
import com.nexusadmin.core.extension.ExtensionRegistry;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * AI 能力装配配置。
 *
 * <p>负责装配 AI 工具注册表、AI 提供者消费者和 LangChain4j ChatLanguageModel 桥接。
 * AiProvider 由插件通过 ExtensionRegistry 动态注册。</p>
 */
@Configuration
public class AiAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(AiAutoConfig.class);

    /**
     * AI 工具注册表默认实现。
     *
     * <p>自动收集所有 Spring 管理的 AiTool Bean，同时支持运行时动态注册。</p>
     *
     * @param springTools Spring 管理的 AiTool Bean 列表（可选，Spring 自动注入）
     * @return AiToolRegistry 实例
     */
    @Bean
    @ConditionalOnMissingBean(AiToolRegistry.class)
    public AiToolRegistry aiToolRegistry(List<AiTool> springTools) {
        log.info("已初始化 DefaultAiToolRegistry，发现 {} 个 Spring 管理的 AiTool Bean",
                springTools != null ? springTools.size() : 0);
        return new DefaultAiToolRegistry(springTools);
    }

    /**
     * AI 提供者动态消费者。
     *
     * <p>封装 ExtensionRegistry 的 AiProvider 消费逻辑，支持运行时热替换。</p>
     *
     * @param extensionRegistry 扩展注册中心
     * @param eventBus          事件总线
     * @return AiProvider 消费者
     */
    @Bean
    @ConditionalOnMissingBean(name = "aiProviderConsumer")
    public ExtensionConsumer<AiProvider> aiProviderConsumer(ExtensionRegistry extensionRegistry,
                                                             EventBus eventBus) {
        return new ExtensionConsumer<>(AiProvider.class, extensionRegistry, eventBus);
    }

    /**
     * LangChain4j ChatLanguageModel 桥接 Bean。
     *
     * <p>动态委托 ExtensionConsumer 获取当前 AiProvider，
     * 对外暴露标准 ChatLanguageModel 接口，便于与 LangChain4j 生态集成。</p>
     *
     * @param aiProviderConsumer AiProvider 动态消费者
     * @return ChatLanguageModel 实例
     */
    @Bean
    @ConditionalOnMissingBean(ChatLanguageModel.class)
    public ChatLanguageModel chatLanguageModel(ExtensionConsumer<AiProvider> aiProviderConsumer) {
        ChatLanguageModel model = new ChatLanguageModel() {
            @Override
            public ChatResponse chat(ChatRequest chatRequest) {
                throw new UnsupportedOperationException("请使用 generate(String) 进行简单文本对话");
            }

            @Override
            public String generate(String userMessage) {
                AiProvider provider = aiProviderConsumer.get()
                        .orElseThrow(() -> new IllegalStateException("没有可用的 AiProvider 扩展实现，请配置 AI 服务插件"));
                return provider.generate(userMessage);
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                AiProvider provider = aiProviderConsumer.get()
                        .orElseThrow(() -> new IllegalStateException("没有可用的 AiProvider 扩展实现，请配置 AI 服务插件"));
                String lastUserMessage = messages.stream()
                        .filter(m -> m instanceof UserMessage)
                        .map(m -> ((UserMessage) m).singleText())
                        .reduce((first, second) -> second)
                        .orElse("");
                String result = provider.generate(lastUserMessage);
                return new Response<>(AiMessage.from(result));
            }
        };
        log.info("已初始化 ChatLanguageModel（动态委托 AiProvider）");
        return model;
    }
}
