# 使用 AI 能力

------

## 1. 通过 MCP 调用 AI 工具

平台内建 MCP JSON-RPC 2.0 端点，所有已注册的 `AiTool` 自动暴露为 MCP Tool。

> 端点地址：`POST /admin/v1/mcp`，要求 `Content-Type: application/json`，需携带认证令牌（Bearer Token）。
> 遵循 JSON-RPC 2.0 规范，请求体需包含 `jsonrpc`、`method`、`id`、`params` 四个字段。

### 1.1 列出所有工具

**请求**：

```http
POST /admin/v1/mcp
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

**响应**：

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "plugin.list",
        "description": "列出所有已安装的插件",
        "inputSchema": { "type": "object", "properties": {} }
      },
      {
        "name": "plugin.start",
        "description": "启动指定插件",
        "inputSchema": {
          "type": "object",
          "properties": {
            "pluginId": { "type": "string" }
          }
        }
      }
    ]
  }
}
```

### 1.2 调用指定工具

**请求**：

```http
POST /admin/v1/mcp
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "plugin.start",
    "arguments": { "pluginId": "system-user" }
  }
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"plugin.start","arguments":{"pluginId":"system-user"}}}'
```

**响应**：

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{\"success\":true,\"message\":\"执行成功\",\"data\":\"插件 system-user 已启动\"}"
      }
    ]
  }
}
```

### 1.3 错误响应

当请求的 Content-Type 不正确或工具不存在时，返回 JSON-RPC 标准错误：

```json
{
  "jsonrpc": "2.0",
  "id": null,
  "error": {
    "code": -32000,
    "message": "未找到工具: unknown.tool"
  }
}
```

### 1.4 权限要求

MCP 端点需要 `system.view` 权限，请求需携带有效的认证令牌。

------

## 2. 对话接口（通过 AiProvider）

当平台安装了 AI 模型插件（实现了 `AiProvider` 的插件）后，可通过注入 `ExtensionConsumer<AiProvider>` 或 `ChatLanguageModel` Bean 发起对话。

### 2.1 通过 AiProvider 调用

```java
@Autowired
private ExtensionConsumer<AiProvider> aiProviderConsumer;

public String askAI(String question) {
    return aiProviderConsumer.get()
            .orElseThrow(() -> new IllegalStateException("未安装 AI 模型插件"))
            .generate(question);
}
```

### 2.2 通过 ChatLanguageModel 调用

```java
@Autowired
private ChatLanguageModel chatLanguageModel;

public String askAI(String question) {
    return chatLanguageModel.generate(question);
}
```

`ChatLanguageModel` 是 LangChain4j 标准接口，可直接传入 `AiServices` 等 LangChain4j 高级组件。

------

## 3. 在 LangChain4j AiServices 中使用

平台装配的 `ChatLanguageModel` 可直接与 LangChain4j 的 `AiServices` 结合：

```java
interface Assistant {
    String chat(String userMessage);
}

@Configuration
public class AiConfig {

    @Bean
    public Assistant assistant(ChatLanguageModel model, AiToolRegistry registry) {
        // 将平台 AiTool 转换为 LangChain4j ToolSpecification
        List<ToolSpecification> tools = registry.listAll().stream()
                .map(tool -> ToolSpecification.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .build())
                .toList();

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
}
```

------

## 4. 查看当前 AI 状态

通过系统状态接口可以查看当前 AI 系统的状态：

- **已安装的 AI 模型插件**：通过 `ExtensionConsumer<AiProvider>.getAll()` 查看
- **已注册的 AI 工具**：通过 `AiToolRegistry.listAll()` 查看所有可用工具
- **MCP 端点状态**：访问 `POST /admin/v1/mcp` 的 `tools/list`

------

## 5. 参考文档

- [AI 体系](../developer-guid/设计文档/AI体系.md)
- [开发 AI 插件](开发AI插件.md)
- [整体架构](../developer-guid/设计文档/整体架构.md)
- [拓展点系统](../developer-guid/设计文档/拓展点系统.md)
