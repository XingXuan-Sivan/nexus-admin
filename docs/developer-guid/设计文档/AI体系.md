# AI 体系

---

## 1. 概述

### 1.1 设计目标

AI 体系是平台的 AI 能力基础设施，提供统一的大模型对话与工具调用抽象，实现 AI 能力的插件化与可替换。其设计目标包括：

- **对话抽象**：通过 `AiProvider` 统一大模型调用接口，屏蔽底层模型差异
- **工具调用**：通过 `AiTool` 定义平台工具，供 AI 在对话中自动选择调用
- **MCP 兼容**：内建 MCP JSON-RPC 端点，所有 AiTool 自动暴露为 MCP Tool
- **LangChain4j 生态对接**：通过 `ChatLanguageModel` 桥接与 LangChain4j 生态无缝集成
- **插件化提供**：AI 能力（模型、工具）以插件形式注册，支持运行时替换

### 1.2 核心原则

- **模型无关**：平台接口不依赖特定模型厂商
- **工具即工具**：所有可被 AI 调用的平台能力封装为 AiTool
- **插件化供给**：AI 模型与工具由插件动态提供
- **协议标准化**：以 MCP JSON-RPC 2.0 为标准调用协议

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

### 2.2 模块分布


| 层次     | 所在模块        | 核心组件                                                             |
| -------- | --------------- | -------------------------------------------------------------------- |
| 接口定义 | nexus-admin-api | `AiProvider`、`AiTool`、`AiToolRegistry`、`EnableAiTools`          |
| 内建实现 | nexus-admin-api | `DefaultAiToolRegistry`、`AiToolAdapter`、`McpController`          |
| 配置桥接 | nexus-admin-api | `AiAutoConfig`（装配 AiToolRegistry、ChatLanguageModel 桥接） |
| 扫描监听 | nexus-admin-app | `AiToolLifecycleListener`（插件 @Tool 注解扫描与注册）              |

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

### 3.5 McpController（MCP 端点）

MCP JSON-RPC 2.0 兼容端点，将所有 `AiTool` 暴露为 MCP Tool。


| 方法         | 说明                                                    |
| ------------ | ------------------------------------------------------- |
| `tools/list` | 列出所有可用工具，返回 name / description / inputSchema |
| `tools/call` | 调用指定工具，传入 name + arguments，返回执行结果       |

端点路径：`POST /admin/v1/mcp`，Content-Type 必须为 `application/json`。

### 3.6 ChatLanguageModel 桥接

`AiAutoConfig` 装配一个 `ChatLanguageModel` Bean，动态委托 `ExtensionConsumer<AiProvider>` 获取当前最高优先级的模型实现。此桥接使平台能力与 LangChain4j 生态无缝互操作——任何接受 `ChatLanguageModel` 的 LangChain4j 组件（如 `AiServices`）均可直接注入使用。

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


| 平台组件          | LangChain4j 对等概念 | 关系                                                                     |
| ----------------- | -------------------- | ------------------------------------------------------------------------ |
| `AiProvider`      | `ChatLanguageModel`  | `AiProvider` 是平台抽象，`ChatLanguageModel` 桥接 Bean 委托 `AiProvider` |
| `AiTool`             | `@Tool` 注解方法     | `AiToolAdapter` 将 `@Tool` 方法适配为 `AiTool`                     |
| `AiToolRegistry`     | `ToolProvider`       | `AiToolRegistry` 是平台级注册表，`McpController` 将其暴露为 MCP Tool    |
| `McpController`   | MCP Server           | 标准 MCP JSON-RPC 2.0 端点，兼容任何 MCP 客户端                          |

插件开发者可以选择：

- 使用 LangChain4j 的 `@Tool` 注解快速定义工具
- 直接实现 `AiTool` 接口获得完全控制
- 基于 LangChain4j 的 `ChatLanguageModel` 实现 `AiProvider`

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
