# AI 体系

---

## 1. 概述

### 1.1 设计目标

AI 体系是平台的 AI 能力基础设施，提供统一的大模型对话与工具调用抽象，实现 AI 能力的插件化与可替换。其设计目标包括：

- **对话抽象**：通过 `AiProvider` 统一大模型调用接口，屏蔽底层模型差异
- **工具调用**：通过 `AiTool` 定义平台工具，供 AI 在对话中自动选择调用
- **MCP 双向通信**：内建 MCP JSON-RPC 服务端端点与客户端连接管理，形成完整 MCP 能力闭环
- **LangChain4j 生态对接**：通过 `ChatLanguageModel` 桥接与 LangChain4j 生态无缝集成
- **插件化提供**：AI 能力（模型、工具）以插件形式注册，支持运行时替换

### 1.2 核心原则

- **模型无关**：平台接口不依赖特定模型厂商
- **工具即工具**：所有可被 AI 调用的平台能力封装为 AiTool
- **插件化供给**：AI 模型与工具由插件动态提供
- **协议标准化**：以 MCP JSON-RPC 2.0 为标准调用协议
- **双向对称**：MCP Server 对外暴露能力，MCP Client 对内引入外部工具

---

## 2. 架构设计

### 2.1 分层结构

```
┌─────────────────────────────────────────────────────────┐
│                    AI 体系三层抽象                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │          第一层：AiProvider（模型提供者）           │  │
│  │  • ExtensionPoint 扩展点                          │  │
│  │  • generate(String prompt) → String               │  │
│  │  • 插件实现可基于 LangChain4j / OpenAI SDK 等      │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │          第二层：AiTool（AI 工具）                    │  │
│  │  • 定义 getName() / getDescription() /            │  │
│  │    getInputTypeSchema()                           │  │
│  │  • execute(Map, InvocationContext) → ToolResult   │  │
│  │  • 兼容 LangChain4j @Tool 注解                    │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │         第三层：MCP JSON-RPC 端点                  │  │
│  │  • POST /admin/v1/mcp                            │  │
│  │  • tools/list / tools/call 标准方法               │  │
│  │  • 所有 AiTool 自动暴露为 MCP Tool                 │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 2.2 MCP 双向通信体系

AI 体系通过 MCP Server + MCP Client + 远程工具桥接 + LangChain4j 集成，形成完整的 MCP 双向通信能力闭环：

```
                          ┌──────────────────────────┐
  外部 AI 工具 ──MCP──────▶│    McpController          │
  (Claude/Cline等)        │    (MCP Server)           │
                          │    POST /admin/v1/mcp     │
                          │    mode=all|local|bridged │
                          └───────────┬──────────────┘
                                      │
                                      ▼
                          ┌──────────────────────────┐
                          │    AiToolRegistry          │
                          │    (工具唯一权威来源)       │
                          │                           │
                          │  ┌─ 本地工具（Spring Bean） │
                          │  ├─ 插件工具（@Tool 注解）  │
                          │  └─ 桥接工具（mcp.* 前缀）  │
                          └──┬───────────┬───────────┘
                             │           │
              ┌──────────────┘           └──────────────┐
              ▼                                         ▼
  ┌───────────────────────┐             ┌───────────────────────────┐
  │ McpRemoteToolBridge    │             │ AIToolProvider             │
  │ (桥接调度器)            │             │ (LangChain4j 工具桥接)      │
  │                       │             │                           │
  │ • 监听注册表变更       │             │ • 实现 ToolProvider 接口    │
  │ • 自动 bridge/unbridge │             │ • 本地+桥接 统一暴露        │
  │ • 维护桥接名称清单     │             │ • @ConditionalOnClass 保护  │
  └───────────┬───────────┘             └───────────────────────────┘
              │
              ▼
  ┌──────────────────────────┐
  │ McpClientConnection       │──MCP──▶ 外部 MCP 服务
  │ (HTTP/stdio/SSE)          │         (代码审查/数据查询/...)
  │                           │
  │ McpRemoteToolAdapter      │
  │ → mcp.{server}.{tool}    │
  └──────────────────────────┘
              │
              ▼
  ┌──────────────────────────┐
  │ McpClientRegistry         │◀── McpClientController
  │ (连接生命周期管理)         │    (管理面板 CRUD API)
  │ McpClientService          │
  └──────────────────────────┘
```

**四条数据通路：**

| 通路 | 方向 | 数据流 | 说明 |
|------|------|--------|------|
| **对外暴露** | 内部 → 外部 | `AiTool` → `AiToolRegistry` → `McpController` → 外部 MCP 客户端 | `tools/list` 支持 `mode=all/local/bridged` 分类过滤 |
| **对内引入** | 外部 → 内部 | 外部 MCP 服务 → `McpClientConnection` → `McpClientRegistry` → 管理面板查看/调用 | 通过 `McpClientController` 管理连接全生命周期 |
| **桥接回路** | 外部 → 内部 | `McpRemoteToolBridge` 自动将启用连接的远程工具 → `McpRemoteToolAdapter` → `AiToolRegistry` | 连接注册时自动桥接，注销时自动解桥；桥接工具与本地工具完全等价 |
| **LangChain4j 集成** | 内部统一 | `AIToolProvider` 将 `AiToolRegistry` 全部工具暴露给 LangChain4j `AiServices` | 开发者注入 `AIToolProvider` 即可使用全部平台工具（本地+桥接） |

**自动装配关系：**

所有 MCP 相关 Bean（Server + Client + Bridge + LangChain4j 集成）统一由 `McpAutoConfig` 装配：

```
McpAutoConfig
├── McpController(AiToolRegistry, McpRemoteToolBridge)  ← MCP Server 端点
├── McpRemoteToolBridge(AiToolRegistry)                  ← 桥接调度器
├── McpClientRegistry(ConfigFacade, McpRemoteToolBridge) ← 客户端注册表
├── McpClientService(McpClientRegistry, McpRemoteToolBridge) ← 业务服务
├── McpClientConnection()                                 ← 默认 HTTP 传输
└── AIToolProvider(AiToolRegistry)                        ← LangChain4j 桥接 (@ConditionalOnClass)
```

### 2.3 模块分布


| 层次     | 所在模块        | 核心组件                                                                                           |
| -------- | --------------- | -------------------------------------------------------------------------------------------------- |
| 接口定义 | nexus-admin-api | `AiProvider`、`AiTool`、`AiToolRegistry`、`EnableAiTools`、`McpClientConnection`、`McpClientRegistry`、`McpRemoteToolBridge` |
| 内建实现 | nexus-admin-api | `DefaultAiToolRegistry`、`AiToolAdapter`、`McpController`、`McpClientController`、`HttpMcpClientConnection`、`DefaultMcpClientRegistry`、`McpRemoteToolAdapter`、`DefaultMcpRemoteToolBridge`、`DefaultMcpClientService` |
| LangChain4j 集成 | nexus-admin-api | `AIToolProvider`（`extension/ai/langchain4j/`，实现 `ToolProvider` 接口）                          |
| 配置装配 | nexus-admin-api | `AiAutoConfig`（AI 核心装配）、`McpAutoConfig`（MCP 全量装配：Server + Client + Bridge + LangChain4j） |
| 扫描监听 | nexus-admin-app | `AiToolLifecycleListener`（插件 @Tool 注解扫描与注册）                                             |

---

## 3. 核心组件

### 3.1 AiProvider（模型提供者）

AI 服务提供者扩展点，定义统一的对话接口。

```java
public interface AiProvider extends ExtensionPoint {
    /** 发送提示词并获取 AI 回复 */
    String generate(String prompt);

    /** 获取模型元信息 */
    AiProviderInfo getInfo();

    record AiProviderInfo(String name, String model, String version) {}
}
```

`AiProvider` 是 `ExtensionPoint` 的子类型，插件通过 `@Extension` 注解注册实现，平台通过 `ExtensionConsumer<AiProvider>` 动态获取最高优先级实现。

**设计考量**：接口仅定义 `generate(String)` 单一方法，避免引入复杂消息模型依赖。插件内部可使用 LangChain4j、OpenAI SDK 等任意实现，通过此接口暴露统一对话能力。

### 3.2 AiTool（AI 工具）

AI 工具接口，定义可被 AI 调用的平台工具。

```java
public interface AiTool {
    /** 工具唯一名称，如 "plugin.list" */
    String getName();

    /** 工具描述，供 AI 理解用途 */
    String getDescription();

    /** 参数 JSON Schema */
    String getInputTypeSchema();

    /** 执行工具 */
    ToolResult execute(Map<String, Object> arguments, InvocationContext context);

    /** JSON 字符串桥接调用 */
    default String call(String jsonInput) { ... }

    record ToolResult(boolean success, String message, Object data) {}
}
```

工具的实现方式有两种：

- **直接实现 AiTool**：适合需要精细控制参数 Schema 和行为的场景
- **@Tool 注解**：在插件组件的方法上标注 `@Tool`，由 `AiToolLifecycleListener` 自动扫描包装为 `AiToolAdapter`

### 3.3 AiToolRegistry（工具注册表）

AI 工具唯一权威来源，聚合 Spring Bean 发现与运行时动态注册。

```java
public interface AiToolRegistry {
    void register(AiTool tool);
    void unregister(String toolName);
    List<AiTool> listAll();
    AiTool get(String toolName);
}
```

`DefaultAiToolRegistry` 合并两个来源：Spring 管理的 `AiTool` Bean（启动时注入）和运行时动态注册的工具（插件激活时由 `AiToolLifecycleListener` 注册）。同名工具以运行时注册为准。

### 3.4 AiToolLifecycleListener（工具扫描监听器）

镜像 `WebEndpointLifecycleListener` 的设计，在插件 `ACTIVE` 时扫描并注册 AI 工具，`STOPPING` 时自动注销。

核心流程：

1. 检测插件主类是否标注 `@EnableAiTools`
2. 使用 `ClassPathScanningCandidateComponentProvider` + 插件 ClassLoader 桥接扫描候选组件
3. 实例化为 `AiTool` 的直接实现，或提取 `@Tool` 方法通过 `AiToolAdapter` 包装
4. 注册到 `AiToolRegistry`

### 3.5 McpController（MCP 服务端端点）

MCP JSON-RPC 2.0 兼容端点，将所有 `AiTool` 暴露为 MCP Tool，支持按来源分类过滤。


| 方法         | 参数            | 说明                                                         |
| ------------ | --------------- | ------------------------------------------------------------ |
| `tools/list` | `mode`（可选）   | 列出可用工具。`all`（默认）列出全部；`local` 仅列出本地工具；`bridged` 仅列出桥接工具 |
| `tools/call` | `name` + `arguments` | 调用指定工具，传入工具名称和参数，返回执行结果                |

端点路径：`POST /admin/v1/mcp`，Content-Type 必须为 `application/json`。
由 `McpAutoConfig` 显式装配，同时注入 `AiToolRegistry` 和 `McpRemoteToolBridge`：
- `AiToolRegistry` 提供全部工具列表
- `McpRemoteToolBridge.getAllBridgedToolNames()` 提供桥接工具名称集合，用于 `mode` 过滤

`tools/call` 统一通过 `AiToolRegistry.get(name)` 执行，不区分工具来源（本地/桥接），保证执行路径一致。

### 3.6 McpClientConnection（MCP 客户端连接）

MCP 客户端连接扩展点，封装与远程 MCP 服务端的通信。

```java
public interface McpClientConnection extends ExtensionPoint {
    String getProtocol();
    void connect(McpConnectionConfig config);
    void disconnect();
    boolean isConnected();
    List<McpRemoteTool> listTools();
    McpToolResult callTool(String toolName, Map<String, Object> arguments);
    McpServerInfo getServerInfo();

    record McpConnectionConfig(String url, String authToken, Map<String, String> headers) {}
    record McpRemoteTool(String name, String description, String inputSchema) {}
    record McpToolResult(boolean success, String content, String error) {}
    record McpServerInfo(String name, String version) {}
}
```

`McpClientConnection` 是 `ExtensionPoint` 的子类型，默认提供 HTTP JSON-RPC 实现（`HttpMcpClientConnection`）。插件可通过注册自定义实现支持 stdio、SSE 等传输协议。

### 3.7 McpClientRegistry（MCP 客户端注册表）

聚合所有已注册的 MCP 客户端连接，提供统一的远程工具查询与调用入口。

```java
public interface McpClientRegistry {
    void register(McpConnectionInfo info);
    void unregister(String connectionId);
    Optional<McpClientConnection> get(String connectionId);
    List<McpConnectionInfo> listAll();
    List<McpClientConnection.McpRemoteTool> listAllRemoteTools();
    McpClientConnection.McpToolResult callRemoteTool(String toolName, Map<String, Object> arguments);
    boolean testConnection(McpConnectionInfo info);
}
```

`DefaultMcpClientRegistry` 启动时从配置中心（作用域 `mcp-clients`）加载已保存的连接列表，运行时自动持久化变更。

### 3.8 McpClientService（MCP 客户端服务）

业务服务接口，封装对注册表的管理操作，提供符合平台规范的 CRUD 方法。

```java
public interface McpClientService {
    List<McpConnectionInfo> list();
    McpConnectionInfo get(String id);
    McpConnectionInfo create(McpConnectionInfo info);
    McpConnectionInfo update(String id, McpConnectionInfo info);
    void delete(String id);
    boolean testConnection(McpConnectionInfo info);
    List<McpClientConnection.McpRemoteTool> listRemoteTools(String connectionId);
    List<McpClientConnection.McpRemoteTool> listAllRemoteTools();
    McpClientConnection.McpToolResult callRemoteTool(String toolName, Map<String, Object> arguments);
    void refreshBridge(String connectionId);
    List<String> getBridgedTools(String connectionId);
}
```

### 3.9 McpClientController（MCP 客户端管理 API）

管理面板 REST API，提供外部 MCP 连接的全生命周期管理。


| 操作         | HTTP 方法 | 路径                                  | 说明                 |
| ------------ | --------- | ------------------------------------- | -------------------- |
| 连接列表     | `GET`     | `/admin/v1/mcp/clients`              | 获取所有连接         |
| 连接详情     | `GET`     | `/admin/v1/mcp/clients/{id}`         | 获取连接详情         |
| 创建连接     | `POST`    | `/admin/v1/mcp/clients`              | 创建新连接           |
| 更新连接     | `PUT`     | `/admin/v1/mcp/clients/{id}`         | 更新连接配置         |
| 删除连接     | `DELETE`  | `/admin/v1/mcp/clients/{id}`         | 删除连接             |
| 测试连接     | `POST`    | `/admin/v1/mcp/clients/{id}/test`    | 测试连接可用性       |
| 远程工具列表 | `GET`     | `/admin/v1/mcp/clients/{id}/tools`   | 浏览远程工具         |
| 刷新桥接     | `POST`    | `/admin/v1/mcp/clients/{id}/bridge/refresh` | 重新拉取并注册远程工具 |
| 桥接工具列表 | `GET`     | `/admin/v1/mcp/clients/{id}/bridged-tools`  | 查看已桥接的工具列表   |

所有 MCP API 统一收敛在 `/admin/v1/mcp` 前缀下，Server 与 Client 形成层次化命名空间。

### 3.10 McpRemoteToolBridge（远程工具桥接调度器）

MCP 远程工具桥接的核心调度器，管理外部 MCP 工具的注册/注销生命周期，是桥接回路的中枢。

```java
public interface McpRemoteToolBridge {
    /** 将指定连接的远程工具桥接到 AiToolRegistry */
    int bridge(McpClientConnection connection, McpConnectionInfo info);

    /** 从 AiToolRegistry 移除指定连接的所有桥接工具 */
    int unbridge(String connectionId);

    /** 刷新指定连接的工具桥接（断开旧桥接，重新拉取并注册） */
    int refresh(McpClientConnection connection, McpConnectionInfo info);

    /** 获取指定连接已桥接的工具名称列表 */
    List<String> getBridgedTools(String connectionId);

    /** 获取所有已桥接的工具名称集合（供 MCP Server 分类过滤） */
    Set<String> getAllBridgedToolNames();

    /** 判断指定连接是否可以执行桥接 */
    boolean canBridge(McpConnectionInfo info);
}
```

`DefaultMcpRemoteToolBridge` 维护 `ConcurrentHashMap<String, List<String>>` 记录每个连接已桥接的工具。

**桥接执行条件（全部满足才执行）：**
- 连接处于 CONNECTED 状态
- 连接配置 `enabled = true`
- 连接配置 `bridgeEnabled = true`
- 远程工具列表非空

**与注册表的集成：** `DefaultMcpClientRegistry` 在 `register()` 成功后自动调用 `bridge.bridge()`，在 `unregister()` 前自动调用 `bridge.unbridge()`。

### 3.11 McpRemoteToolAdapter（远程工具适配器）

将远程 MCP 工具包装为平台 `AiTool`，使平台 AI 能透明地调用外部 MCP 工具。

- 远程工具名称自动添加 `mcp.{server}.{tool}` 前缀，避免与本地工具命名冲突
- `execute()` 方法通过 `McpClientConnection.callTool()` 调用远程 MCP 服务
- 由 `McpRemoteToolBridge` 在桥接时创建并注册到 `AiToolRegistry`

### 3.12 ChatLanguageModel 桥接

`AiAutoConfig` 装配一个 `ChatLanguageModel` Bean，动态委托 `ExtensionConsumer<AiProvider>` 获取当前最高优先级的模型实现。此桥接使平台能力与 LangChain4j 生态无缝互操作——任何接受 `ChatLanguageModel` 的 LangChain4j 组件（如 `AiServices`）均可直接注入使用。

### 3.13 AIToolProvider（LangChain4j 工具桥接）

平台级 LangChain4j `ToolProvider` 桥接实现，将 `AiToolRegistry` 中所有工具（本地 + 桥接）无缝暴露给 LangChain4j 生态。

```java
public class AIToolProvider implements ToolProvider {
    private final AiToolRegistry toolRegistry;

    /** 向 LangChain4j 提供所有可用工具的映射 */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        for (AiTool tool : toolRegistry.listAll()) {
            ToolSpecification spec = ToolSpecification.builder()
                    .name(tool.getName())
                    .description(tool.getDescription())
                    .parameters(buildSchema(tool.getInputTypeSchema()))
                    .build();
            ToolExecutor executor = (toolRequest, memoryId) -> {
                Map<String, Object> args = JSON.parseObject(toolRequest.arguments(), Map.class);
                AiTool.ToolResult result = tool.execute(args, context);
                return result.message();
            };
            tools.put(spec, executor);
        }
        return new ToolProviderResult(tools);
    }
}
```

**核心特性：**
- 实现 `dev.langchain4j.service.tool.ToolProvider` 接口，与 LangChain4j 生态无缝对接
- 以 `AiToolRegistry` 为唯一工具权威来源，本地工具和桥接工具对 LangChain4j 完全透明
- 通过 `@ConditionalOnClass("dev.langchain4j.service.AiServices")` 条件装配，未引入 LangChain4j 的项目零影响
- 带 `@ConditionalOnMissingBean` 保护，插件可覆盖提供自定义 ToolProvider

**使用示例：**
```java
@Autowired ChatLanguageModel model;
@Autowired AIToolProvider toolProvider;

Assistant ai = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .toolProvider(toolProvider)
        .build();

String result = ai.chat("审查 Service.java");
// AI 自动发现所有平台工具（包括桥接的 mcp.* 工具）并调用
```

---

## 4. 插件 AI 能力注册流程

```
插件 jar 被加载
      │
      ▼
插件进入 ACTIVE 状态
      │
      ▼
AiToolLifecycleListener 检测 @EnableAiTools
      │
      ├─── 扫描 @Component + AiTool 实现 ──► 直接注册到 AiToolRegistry
      │
      └─── 扫描 @Tool 注解方法 ──► AiToolAdapter ──► 注册到 AiToolRegistry
      │
      ▼
McpController 自动发现新工具
      │
      ▼
MCP 客户端可通过 tools/list 获取完整工具列表
```

---

## 5. 与 LangChain4j 生态的关系

平台 AI 接口完全独立于 LangChain4j，但通过桥接层实现深度互操作：


| 平台组件              | LangChain4j 对等概念 | 关系                                                                         |
| --------------------- | -------------------- | ---------------------------------------------------------------------------- |
| `AiProvider`          | `ChatLanguageModel`  | `AiProvider` 是平台抽象，`ChatLanguageModel` 桥接 Bean 委托 `AiProvider`     |
| `AiTool`              | `@Tool` 注解方法     | `AiToolAdapter` 将 `@Tool` 方法适配为 `AiTool`                               |
| `AiToolRegistry`      | `ToolProvider`       | `AiToolRegistry` 是平台级注册表，通过 `AIToolProvider` 桥接到 LangChain4j    |
| `AIToolProvider`      | `ToolProvider`       | 平台级 `ToolProvider` 实现，将全部 `AiTool`（本地+桥接）暴露给 LangChain4j    |
| `McpController`       | MCP Server           | 标准 MCP JSON-RPC 2.0 端点，兼容任何 MCP 客户端                              |
| `McpRemoteToolBridge` | -                    | 远程工具桥接调度器，对外部 MCP 服务的工具自动注册到 `AiToolRegistry`          |

插件开发者可以选择：

- 使用 LangChain4j 的 `@Tool` 注解快速定义工具
- 直接实现 `AiTool` 接口获得完全控制
- 基于 LangChain4j 的 `ChatLanguageModel` 实现 `AiProvider`
- 注入 `AIToolProvider` 在 `AiServices` 中直接使用全部平台工具（无需手动映射）
- 通过 `McpAutoConfig` 自动获得 MCP Server + Client + Bridge + LangChain4j 完整能力

---

## 6. 设计约束与实践规范

### 6.1 AiProvider 实现规范

- 必须实现 `generate(String)` 返回纯文本回复
- 必须提供有意义的 `AiProviderInfo`
- 通过 `@Extension` 注解注册，合理设置 `priority`

### 6.2 AiTool 实现规范

- 工具名称使用命名空间格式，如 `plugin.list`、`config.get`
- `getDescription()` 应包含工具用途和参数说明
- `getInputTypeSchema()` 返回标准 JSON Schema 字符串
- 执行结果通过 `ToolResult` 返回，`success=false` 表示执行失败

### 6.3 @Tool 注解规范

```java
@Component
public class PluginTools {

    @Tool(name = "plugin.start", value = "启动指定插件，参数 pluginId 为插件ID")
    public String startPlugin(String pluginId) {
        // 实现逻辑
        return "插件 " + pluginId + " 已启动";
    }
}
```

- 方法所在类须为 `@Component`
- `@Tool.name` 指定工具唯一名称
- `@Tool.value` 指定工具描述
- 参数类型应为基础类型（String、int、boolean 等）

---

AI 体系以平台接口为契约、LangChain4j 为生态桥接、MCP 为标准化调用协议，实现了 AI 对话与工具能力的插件化供给与运行时替换。
