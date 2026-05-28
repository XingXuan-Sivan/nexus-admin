# 使用 AI 能力

------

## 1. 通过 MCP 调用 AI 工具

平台内建 MCP JSON-RPC 2.0 端点，所有已注册的 `AiTool` 自动暴露为 MCP Tool。

> 端点地址：`POST /admin/v1/mcp`，要求 `Content-Type: application/json`，需携带认证令牌（Bearer Token）。
> 遵循 JSON-RPC 2.0 规范，请求体需包含 `jsonrpc`、`method`、`id`、`params` 四个字段。

### 1.1 列出所有工具

`tools/list` 支持可选的 `mode` 参数，用于按工具来源分类过滤：

| mode 值   | 说明                               |
| --------- | ---------------------------------- |
| `all`     | 默认值，列出全部工具（本地+桥接）   |
| `local`   | 仅列出平台自带的本地工具            |
| `bridged` | 仅列出从外部 MCP 服务桥接来的工具   |

**请求（默认 all）**：

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

### 1.3 按模式过滤工具列表

**仅列出本地工具**：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{"mode":"local"}}'
```

**仅列出桥接工具**：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{"mode":"bridged"}}'
```

### 1.4 错误响应

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

### 1.5 权限要求

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

平台提供 `AIToolProvider` Bean（条件装配，需引入 LangChain4j 依赖），开发者注入后即可在 `AiServices` 中直接使用全部平台工具（本地 + 桥接），无需手动映射。

```java
interface Assistant {
    String chat(String userMessage);
}

@Configuration
public class AiConfig {

    @Bean
    public Assistant assistant(ChatLanguageModel model, AIToolProvider toolProvider) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .toolProvider(toolProvider)          // ← 一行注入，自动发现全部工具
                .build();
    }
}
```

`AIToolProvider` 以 `AiToolRegistry` 为唯一权威来源，每次 `AiServices` 请求工具列表时动态获取最新工具（包括运行时注册/注销的桥接工具），确保工具列表始终与注册表同步。

------

## 4. 查看当前 AI 状态

通过系统状态接口可以查看当前 AI 系统的状态：

- **已安装的 AI 模型插件**：通过 `ExtensionConsumer<AiProvider>.getAll()` 查看
- **已注册的 AI 工具**：通过 `AiToolRegistry.listAll()` 查看所有可用工具
- **MCP 端点状态**：访问 `POST /admin/v1/mcp` 的 `tools/list`

------

## 5. 管理 MCP 客户端连接

平台提供管理面板 API 用于管理外部 MCP 服务连接，实现对远程 MCP 工具的统一纳管与调用。

> 管理 API 基础路径：`/admin/v1/mcp/clients`，需携带认证令牌。所有 MCP 相关 API 统一收敛在 `/admin/v1/mcp` 前缀下。

### 5.1 连接管理

**列出所有连接**：

```bash
curl http://localhost:8080/admin/v1/mcp/clients \
  -H "Authorization: Bearer <your-token>"
```

**创建新连接**：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "name": "外部工具服务",
    "url": "http://remote-host:9090/admin/v1/mcp",
    "protocol": "http",
    "authToken": "",
    "enabled": true,
    "bridgeEnabled": true,
    "bridgeMode": "ALL"
  }'
```

连接配置中可选的桥接控制字段：
- `bridgeEnabled`（默认 `true`）：是否自动将远程工具桥接到 `AiToolRegistry`
- `bridgeMode`（默认 `ALL`）：桥接模式，`ALL` 桥接所有远程工具

**更新连接**：

```bash
curl -X PUT http://localhost:8080/admin/v1/mcp/clients/{connection-id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "name": "更新后的名称",
    "url": "http://new-host/mcp",
    "protocol": "http",
    "enabled": true
  }'
```

**删除连接**：

```bash
curl -X DELETE http://localhost:8080/admin/v1/mcp/clients/{connection-id} \
  -H "Authorization: Bearer <your-token>"
```

### 5.2 连接测试

创建连接前可以先测试连接是否可用：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp/clients/test-connection/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "url": "http://remote-host:9090/admin/v1/mcp",
    "protocol": "http"
  }'
```

### 5.3 浏览远程工具

建立连接后，可浏览远程 MCP 服务端提供的工具：

```bash
curl http://localhost:8080/admin/v1/mcp/clients/{connection-id}/tools \
  -H "Authorization: Bearer <your-token>"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "name": "code.review",
      "description": "执行代码审查并返回审查报告",
      "inputSchema": "{...}"
    },
    {
      "name": "data.query",
      "description": "查询业务数据",
      "inputSchema": "{...}"
    }
  ]
}
```

### 5.4 调用远程工具

通过管理 API 手动调用远程 MCP 工具：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp/clients/{connection-id}/tools/call \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "toolName": "code.review",
    "arguments": {
      "filePath": "/src/main/Service.java"
    }
  }'
```

### 5.5 远程工具桥接管理

平台在连接注册成功后自动将远程工具桥接到本地 `AiToolRegistry`，也可手动管理桥接：

**刷新桥接**（重新拉取远程工具列表并注册）：

```bash
curl -X POST http://localhost:8080/admin/v1/mcp/clients/{connection-id}/bridge/refresh \
  -H "Authorization: Bearer <your-token>"
```

**查看已桥接的工具**：

```bash
curl http://localhost:8080/admin/v1/mcp/clients/{connection-id}/bridged-tools \
  -H "Authorization: Bearer <your-token>"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "mcp.我的外部服务.code.review",
    "mcp.我的外部服务.data.query"
  ]
}
```

桥接后的工具通过 `AiToolRegistry` 统一管理，命名规则为 `mcp.{serverName}.{remoteToolName}`。桥接工具与本地工具在 MCP Server 和 LangChain4j 中完全等价。

### 5.6 权限要求

| 操作类型     | 所需权限              |
| ------------ | --------------------- |
| 查看连接列表 | `mcp-clients.view`   |
| 测试/浏览/调用 | `mcp-clients.view` |
| 创建/更新/删除 | `mcp-clients.manage` |

------

## 6. 参考文档

- [AI 体系](../developer-guid/设计文档/AI体系.md)
- [开发 AI 插件](开发AI插件.md)
- [整体架构](../developer-guid/设计文档/整体架构.md)
- [拓展点系统](../developer-guid/设计文档/拓展点系统.md)
