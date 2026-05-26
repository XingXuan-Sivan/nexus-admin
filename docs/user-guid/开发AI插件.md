# 开发 AI 插件

------

## 1. 概述

AI 插件是提供 AI 模型或 AI 工具能力的插件。平台支持三种 AI 插件开发模式：

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| 模型提供插件 | 实现 `AiProvider`，提供大模型对话能力 | 接入 OpenAI、Ollama、通义千问等模型 |
| 工具提供插件 | 实现 `AiTool` 或使用 `@Tool` 注解，提供 AI 可调用的工具 | 提供插件管理、配置读写、日志查询等平台操作能力 |
| 混合插件 | 同时提供模型和工具能力 | 完整的 AI 解决方案插件 |

------

## 2. 开发模型提供插件

### 2.1 添加依赖

```xml
<dependency>
    <groupId>com.nexusadmin</groupId>
    <artifactId>nexus-admin-api</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- LangChain4j 模型实现（以 Ollama 为例） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
</dependency>
```

### 2.2 实现 AiProvider

```java
import com.nexusadmin.api.extension.ai.AiProvider;
import com.nexusadmin.core.extension.Extension;
import dev.langchain4j.model.ollama.OllamaChatModel;

@Extension(points = AiProvider.class, priority = 100, name = "Ollama AI 提供者")
public class OllamaAiProvider implements AiProvider {

    private final OllamaChatModel model;

    public OllamaAiProvider() {
        this.model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen2.5:7b")
                .build();
    }

    @Override
    public String generate(String prompt) {
        return model.generate(prompt);
    }

    @Override
    public AiProviderInfo getInfo() {
        return new AiProviderInfo("ollama", "qwen2.5:7b", "1.0.0");
    }
}
```

### 2.3 插件主类

```java
@EnableAiTools
@EnableWebEndpoints
public class OllamaAiPlugin extends AbstractPlugin {

    @Override
    protected void start() throws Exception {
        extensions().register(AiProvider.class, new OllamaAiProvider(), 100);
    }

    @Override
    protected void stop() throws Exception {
        // 清理资源
    }
}
```

------

## 3. 开发工具提供插件

### 3.1 方式一：使用 @Tool 注解（推荐）

在 `@Component` 类的方法上标注 LangChain4j 的 `@Tool` 注解，由平台自动扫描注册。

```java
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class PluginManagementTools {

    @Tool(name = "plugin.list", value = "列出所有已安装的插件")
    public String listPlugins() {
        // 获取并返回插件列表
        return "当前已安装插件: system-user, demo-plugin";
    }

    @Tool(name = "plugin.start", value = "启动指定插件，参数 pluginId 为插件唯一标识")
    public String startPlugin(String pluginId) {
        // 启动插件逻辑
        return "插件 " + pluginId + " 已启动";
    }

    @Tool(name = "plugin.stop", value = "停止指定插件，参数 pluginId 为插件唯一标识")
    public String stopPlugin(String pluginId) {
        // 停止插件逻辑
        return "插件 " + pluginId + " 已停止";
    }
}
```

插件主类标注 `@EnableAiTools` 即可启用自动扫描：

```java
@EnableAiTools
public class PluginToolsPlugin extends AbstractPlugin {
    // 无需手动注册，@Tool 方法自动发现
}
```

### 3.2 方式二：直接实现 AiTool

适合需要精细控制参数 Schema 和行为的场景。

```java
import com.nexusadmin.api.extension.ai.AiTool;
import com.nexusadmin.api.context.InvocationContext;

public class ConfigGetTool implements AiTool {

    @Override
    public String getName() {
        return "config.get";
    }

    @Override
    public String getDescription() {
        return "读取指定配置项的值";
    }

    @Override
    public String getInputTypeSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "key": { "type": "string", "description": "配置项键名" }
              },
              "required": ["key"]
            }""";
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, InvocationContext context) {
        String key = (String) arguments.get("key");
        String value = "配置值"; // 实际从配置中心读取
        return new ToolResult(true, "读取成功", value);
    }
}
```

在插件主类中手动注册：

```java
public class ConfigToolsPlugin extends AbstractPlugin {

    @Override
    protected void start() throws Exception {
        AiToolRegistry registry = getContext().getBean(AiToolRegistry.class);
        registry.register(new ConfigGetTool());
    }

    @Override
    protected void stop() throws Exception {
        AiToolRegistry registry = getContext().getBean(AiToolRegistry.class);
        registry.unregister("config.get");
    }
}
```

------

## 4. 开发混合插件

混合插件同时提供模型和工具能力：

```java
@EnableAiTools
@EnableWebEndpoints
public class FullAiPlugin extends AbstractPlugin {

    @Override
    protected void start() throws Exception {
        // 注册模型提供者
        extensions().register(AiProvider.class, new OllamaAiProvider(), 100);

        // @Tool 方法由 @EnableAiTools 自动扫描，无需手动注册
    }

    @Override
    protected void stop() throws Exception {
        // 清理
    }
}
```

------

## 5. 创建 plugin.json

```json
{
  "id": "ollama-ai-plugin",
  "version": "1.0.0",
  "mainClass": "com.example.ai.OllamaAiPlugin",
  "dependencies": {}
}
```

------

## 6. @Tool 注解规范

### 6.1 命名规范

- 使用命名空间格式：`{domain}.{action}`，如 `plugin.list`、`config.get`
- 名称应简洁明了，避免过长的英文名称

### 6.2 参数规范

- 参数类型使用基础类型：`String`、`int`、`long`、`boolean`
- 复杂对象建议使用 JSON 字符串传递，在方法内部解析
- `@Tool.value` 应包含每个参数的用途说明

### 6.3 返回值规范

- 返回 `String` 类型，包含执行结果的文字说明
- 失败时抛出异常，平台自动封装为 `ToolResult(success=false)`

------

## 7. 配置 AI 插件

在应用配置文件中可配置 AI 插件相关参数：

```yaml
panel:
  ai:
    # 默认模型提供者优先级阈值（低于此值的实现不被选中）
    min-priority: 50
```

------

## 8. 参考文档

- [AI 体系](../developer-guid/设计文档/AI体系.md)
- [使用 AI 能力](使用AI能力.md)
- [开发插件](开发插件.md)
- [拓展点系统](../developer-guid/设计文档/拓展点系统.md)
- [插件生命周期](../developer-guid/设计文档/插件生命周期.md)
