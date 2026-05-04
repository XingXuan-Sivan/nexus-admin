# 插件 UI 贡献机制

------

## 1. 概述

### 1.1 设计目标

插件 UI 贡献机制是 Nexus Admin 插件化架构中实现插件前端能力扩展的基础设施。在已有 Web 接入机制（后端 API 动态注册）的基础上，本机制让插件能够以**声明式**方式贡献前端 UI 能力，实现管理面板的完整插件化扩展。

设计目标：

- **声明式贡献**：插件通过 `plugin.json` 中的 `contributes` 字段声明 UI 能力，无需编写前端框架代码
- **动态渲染**：前端根据后端聚合的 UI 贡献声明（manifest）动态生成菜单、路由和挂载点内容
- **隔离安全**：插件 UI 资源运行在独立命名空间，防止路径冲突和样式污染
- **权限联动**：UI 贡献与权限系统深度集成，无权限时自动隐藏或拦截
- **渐进增强**：支持从 iframe 静态资源托管向 Module Federation 等高级方案演进

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **声明式优先** | 插件通过 JSON 声明 UI 贡献，而非编写前端框架代码 |
| **静态可分析** | `contributes` 在 `plugin.json` 中静态定义，平台加载阶段即可解析，无需等待插件启动 |
| **隔离安全** | 插件路由强制前缀隔离，资源访问受控，防止冲突和越权 |
| **渐进增强** | 第一阶段基于 iframe + 静态资源托管，远期支持 Module Federation 等高级方案 |

### 1.3 方案对比

| 特性 | Nexus Admin contributes | VS Code contributes | Backstage dynamicRoutes |
|------|------------------------|---------------------|------------------------|
| 配置位置 | `plugin.json` 静态声明 | `package.json` 静态声明 | 代码动态注册 |
| 加载时机 | 插件加载阶段解析 | 扩展安装时解析 | 前端运行时动态加载 |
| 资源隔离 | iframe / 独立路由前缀 | WebView / iframe | 模块联邦（无强隔离） |
| 前端框架 | 框架无关（iframe 方案） | 框架无关 | React 深度绑定 |
| 权限集成 | 声明式权限 + 拓展点校验 | 命令级权限 | RBAC 集成 |
| 演进路线 | iframe → Module Federation | WebView | Module Federation |

Nexus Admin 选择**静态声明 + iframe 沙箱**作为第一阶段方案，原因是：

1. 与现有 `plugin.json` 描述文件体系自然衔接
2. iframe 提供最强的样式和运行隔离，避免插件前端代码污染主框架
3. 声明式配置在插件加载阶段即可解析，无需等待插件启动完成
4. 前端框架无关，不绑定具体技术栈

### 1.4 整体架构

```text
┌─────────────────────────────────────────────────────────────────┐
│                         前端框架层                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   菜单渲染   │  │   路由注册   │  │      挂载点填充          │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
│         │                │                      │                │
│         ▼                ▼                      ▼                │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              GET /admin/v1/ui/manifest                       ││
│  │         聚合所有活跃插件的 contributes 数据                   ││
│  └─────────────────────────┬───────────────────────────────────┘│
└────────────────────────────┼────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         后端聚合层                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              UIContributionRegistry                       │   │
│  │  ┌────────────┐ ┌────────────┐ ┌──────────────────────┐  │   │
│  │  │   menus    │ │   routes   │ │    mountPoints       │  │   │
│  │  └────────────┘ └────────────┘ └──────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             │                                    │
│                             ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              PermissionDeclarationRegistry                │   │
│  │              ┌──────────────────────────────┐             │   │
│  │              │         permissions          │             │   │
│  │              └──────────────────────────────┘             │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                             ▲
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                      插件加载层                                  │
│  ┌─────────────────┐       │      ┌─────────────────────────┐   │
│  │  PluginManager  │       │      │  PluginDescriptorParser │   │
│  │    加载插件      │───────┘      │  解析 plugin.json       │   │
│  │  LOADED → INIT  │              │  提取 contributes 字段   │   │
│  └─────────────────┘              └─────────────────────────┘   │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                      plugin.json                             ││
│  │  {                                                          ││
│  │    "id": "analytics-plugin",                                ││
│  │    "contributes": {                                         ││
│  │      "menus": [...],                                        ││
│  │      "routes": [...],                                       ││
│  │      "mountPoints": [...],                                  ││
│  │      "permissions": [...]                                   ││
│  │    }                                                        ││
│  │  }                                                          ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

------

## 2. contributes 字段规范

### 2.1 完整结构定义

`contributes` 是 `plugin.json` 的**顶级字段**，与 `id`、`version`、`mainClass` 等字段同级。

```json
{
  "id": "analytics-plugin",
  "version": "1.0.0",
  "mainClass": "com.nexusadmin.plugin.analytics.AnalyticsPlugin",
  "description": "数据分析插件",
  "contributes": {
    "menus": [],
    "routes": [],
    "mountPoints": [],
    "permissions": []
  }
}
```

### 2.2 解析原则

| 原则 | 说明 |
|------|------|
| **加载阶段解析** | 平台在插件状态从 `LOADED` 迁移到 `INITIALIZED` 时解析 `contributes`，无需等待插件启动完成 |
| **数组结构** | `menus`、`routes`、`mountPoints`、`permissions` 均为数组，缺失视为空数组 |
| **自动清理** | 插件停止或卸载时，平台自动移除其所有 UI 贡献，无需插件手动处理 |
| **ID 冲突处理** | 相同 ID 的菜单项、权限等，后注册的覆盖先注册的（正常不应出现重复） |

------

## 3. 菜单贡献协议（menus）

### 3.1 声明格式

```json
{
  "contributes": {
    "menus": [
      {
        "id": "analytics.dashboard",
        "label": "数据分析",
        "icon": "chart-line",
        "path": "/plugins/analytics/dashboard",
        "order": 100,
        "parent": "root",
        "permissions": ["analytics.view"],
        "badge": {
          "type": "dot",
          "api": "/api/analytics/unread-count"
        }
      },
      {
        "id": "analytics.reports",
        "label": "报表管理",
        "icon": "file-text",
        "path": "/plugins/analytics/reports",
        "order": 110,
        "parent": "analytics.dashboard",
        "permissions": ["analytics.reports.view"]
      }
    ]
  }
}
```

### 3.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `string` | 是 | 菜单唯一标识，推荐格式 `{pluginId}.{name}` |
| `label` | `string` | 是 | 菜单显示文本 |
| `icon` | `string` | 否 | 图标标识，使用平台内置图标库名称 |
| `path` | `string` | 是 | 点击后导航的路由路径 |
| `order` | `number` | 是 | 排序权重（0-999），数值越小越靠前 |
| `parent` | `string` | 否 | 父菜单 ID，支持最多 3 级嵌套。顶级菜单设为 `"root"` 或不填 |
| `permissions` | `string[]` | 否 | 访问此菜单所需的权限列表（AND 关系），用户无权限时菜单隐藏 |
| `badge` | `object` | 否 | 角标配置，详见下方 |

#### badge 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | `string` | 是 | 角标类型：`dot`（红点）、`number`（数字）、`text`（文本） |
| `api` | `string` | 否 | 动态获取角标数据的 API 路径，前端轮询或监听更新 |

### 3.3 平台内建菜单区域

| parent 值 | 说明 | 内建菜单示例 |
|-----------|------|-------------|
| `root` | 顶级菜单区 | 首页仪表盘 |
| `system` | 系统管理区 | 插件管理、配置中心、系统状态 |
| `business` | 业务功能区 | 插件贡献的业务菜单主要放在此处 |

### 3.4 菜单合并规则

1. **排序规则**：同一 `parent` 下按 `order` 升序排列
2. **冲突处理**：相同 `id` 后注册的覆盖先注册的（正常不应出现重复 ID）
3. **生命周期绑定**：插件停止时其菜单项自动从注册表中移除
4. **权限过滤**：前端根据当前用户权限列表过滤不可见菜单
5. **嵌套限制**：最多支持 3 级嵌套（parent → child → grandchild），超出层级的菜单将被忽略

------

## 4. 路由贡献协议（routes）

### 4.1 声明格式

```json
{
  "contributes": {
    "routes": [
      {
        "path": "/plugins/analytics/dashboard",
        "title": "数据分析仪表盘",
        "component": "analytics-plugin/pages/Dashboard",
        "layout": "default",
        "permissions": ["analytics.view"],
        "props": {
          "refreshInterval": 30
        },
        "meta": {
          "breadcrumb": ["数据分析", "仪表盘"],
          "cache": true
        }
      }
    ]
  }
}
```

### 4.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `path` | `string` | 是 | 路由路径。**必须以 `/plugins/{pluginId}/` 为前缀**，平台强制校验 |
| `title` | `string` | 是 | 页面标题，显示在浏览器标签页和面包屑导航中 |
| `component` | `string` | 是 | 组件标识，格式为 `{pluginId}/{componentPath}` |
| `layout` | `string` | 否 | 布局模板，默认 `default`（带侧边栏 + 顶栏），可选 `blank`（全屏无框架） |
| `permissions` | `string[]` | 否 | 访问此路由所需的权限列表（AND 关系） |
| `props` | `object` | 否 | 传递给组件的静态属性 |
| `meta` | `object` | 否 | 路由元信息，前端框架可自定义消费 |

#### component 字段规则

| 阶段 | 格式 | 解析方式 |
|------|------|----------|
| 第一阶段（iframe） | `{pluginId}/{componentPath}` | 平台自动拼接为 `/plugins/{pluginId}/assets/{componentPath}.html` |
| 第二阶段（Module Federation） | `{pluginId}/{componentPath}` | 指向远程模块的导出名，由模块联邦运行时解析 |

### 4.3 路由隔离规则

1. **前缀强制**：插件路由路径**必须**以 `/plugins/{pluginId}/` 为前缀，否则平台在解析阶段拒绝注册
2. **命名空间隔离**：每个插件的路由在独立命名空间中，不同插件之间不会发生冲突
3. **精确清理**：插件卸载时，平台根据 `pluginId` 精确清理该插件注册的所有路由
4. **权限拦截**：用户无权限访问路由时，前端显示 403 页面，后端 API 返回 403 状态码

------

## 5. 挂载点贡献协议（mountPoints）

### 5.1 声明格式

```json
{
  "contributes": {
    "mountPoints": [
      {
        "slot": "dashboard.widgets",
        "component": "analytics-plugin/components/SummaryWidget",
        "order": 50,
        "permissions": ["analytics.view"],
        "props": {
          "title": "数据概览",
          "size": "medium"
        }
      }
    ]
  }
}
```

### 5.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `slot` | `string` | 是 | 挂载点标识，插件向该插槽注入内容 |
| `component` | `string` | 是 | 组件标识，规则与路由的 `component` 字段相同 |
| `order` | `number` | 否 | 在同一 slot 中的排序权重，默认 100，数值越小越靠前 |
| `permissions` | `string[]` | 否 | 显示此挂载内容所需的权限列表 |
| `props` | `object` | 否 | 传递给组件的属性 |

### 5.3 平台预留挂载点列表

| 挂载点 ID | 位置 | 说明 |
|-----------|------|------|
| `dashboard.widgets` | 首页仪表盘 | 仪表盘卡片 / 小部件 |
| `dashboard.quickActions` | 首页快捷操作区 | 快捷操作按钮 |
| `plugin.detail.tabs` | 插件详情页 | 插件详情的额外 Tab 页 |
| `settings.panels` | 系统设置页 | 额外的设置面板 |
| `user.profile.sections` | 用户个人中心 | 个人中心扩展区域 |
| `header.actions` | 顶部操作栏 | 全局操作按钮 |

### 5.4 挂载点渲染规则

1. **排序渲染**：同一 `slot` 下的挂载项按 `order` 升序排列后依次渲染
2. **权限过滤**：用户无权限时，该挂载项不渲染（不占位）
3. **生命周期绑定**：插件停止时，其挂载项自动从对应 slot 中移除
4. **重复处理**：同一插件在同一 slot 中可注册多个挂载项，按 `order` 排序

------

## 6. 权限声明协议（permissions）

### 6.1 声明格式

```json
{
  "contributes": {
    "permissions": [
      {
        "id": "analytics.view",
        "label": "查看分析数据",
        "description": "允许访问数据分析仪表盘和报表",
        "group": "数据分析"
      },
      {
        "id": "analytics.export",
        "label": "导出分析报告",
        "description": "允许导出分析报告为 PDF/Excel",
        "group": "数据分析",
        "dependencies": ["analytics.view"]
      }
    ]
  }
}
```

### 6.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `string` | 是 | 权限唯一标识，格式 `{pluginId}.{action}` |
| `label` | `string` | 是 | 权限显示名称 |
| `description` | `string` | 否 | 详细描述，用于权限管理界面展示 |
| `group` | `string` | 否 | 权限分组名称，用于管理界面按组展示 |
| `dependencies` | `string[]` | 否 | 依赖的其他权限 ID，授予此权限时自动授予依赖权限 |

### 6.3 权限与 UI 的关联

| UI 元素 | 权限控制行为 |
|---------|-------------|
| `menus` | 用户无权限时，菜单项不显示 |
| `routes` | 用户无权限时，路由拦截并显示 403 页面 |
| `mountPoints` | 用户无权限时，挂载内容不渲染 |

### 6.4 权限校验流程

```text
用户操作/UI 渲染
      │
      ▼
┌─────────────────┐
│ 前端权限预过滤   │  ── 根据本地缓存的权限列表过滤菜单和挂载点
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 后端权限最终校验 │  ── 由 PermissionResolver 拓展点实现最终校验
└────────┬────────┘
         │
         ▼
    允许/拒绝
```

权限最终由 `PermissionResolver` 拓展点实现进行校验，插件可自定义权限校验逻辑。

------

## 7. 前端资源打包与加载方案

### 7.1 第一阶段：静态资源托管 + iframe 沙箱

#### 7.1.1 插件前端资源结构

插件前端资源放置于插件项目的 `src/main/resources/static/` 目录下：

```text
analytics-plugin/
├── src/main/
│   ├── java/
│   │   └── com/nexusadmin/plugin/analytics/
│   │       └── AnalyticsPlugin.java
│   └── resources/
│       ├── META-INF/
│       │   ├── plugin.json          # 含 contributes 声明
│       │   ├── schema.json         # 配置 Schema，采用标准 JSON Schema（Draft 2020-12）格式
│       │   └── config.yml
│       └── static/                  # 前端静态资源
│           ├── pages/
│           │   └── Dashboard.html
│           ├── components/
│           │   └── SummaryWidget.html
│           ├── js/
│           │   └── app.js
│           └── css/
│               └── style.css
```

#### 7.1.2 资源访问路径

平台提供统一端点供前端访问插件静态资源：

```text
/plugins/{pluginId}/assets/**     →  插件 JAR 内 static/ 目录或插件数据目录下的静态资源
```

**示例**：

| 资源文件 | 访问路径 |
|----------|----------|
| `static/pages/Dashboard.html` | `/plugins/analytics-plugin/assets/pages/Dashboard.html` |
| `static/js/app.js` | `/plugins/analytics-plugin/assets/js/app.js` |
| `static/css/style.css` | `/plugins/analytics-plugin/assets/css/style.css` |

#### 7.1.3 iframe 加载流程

```text
用户点击菜单 /plugins/analytics/dashboard
              │
              ▼
    前端路由匹配到插件路由
              │
              ▼
    主框架创建 iframe 容器
              │
              ▼
    iframe src = /plugins/analytics-plugin/assets/pages/Dashboard.html
              │
              ▼
    iframe 内页面加载并执行
              │
              ▼
    平台 JS Bridge SDK 注入 iframe
              │
              ▼
    iframe 内页面通过 Bridge 与主框架通信
```

#### 7.1.4 JS Bridge SDK

平台在每个 iframe 加载时自动注入 `window.NexusAdmin` 对象，提供以下 API：

```javascript
// 由平台注入到每个 iframe 中
window.NexusAdmin = {
  // 获取当前认证 Token
  getToken: function() { /* Promise<string> */ },

  // 导航到指定路由
  navigate: function(path) { /* void */ },

  // 显示全局通知
  notify: function(options) {
    // options: { type: 'success'|'error'|'info', message: string }
  },

  // 获取平台信息
  getPlatformInfo: function() { /* Promise<PlatformInfo> */ },

  // 调用平台 API（自动附加 Token）
  fetch: function(url, options) { /* Promise<Response> */ },

  // 监听平台事件
  on: function(event, callback) { /* void */ },

  // 调整 iframe 高度（自适应内容）
  resizeFrame: function(height) { /* void */ },

  // 获取当前用户信息
  getCurrentUser: function() { /* Promise<UserInfo> */ },

  // 获取插件配置
  getConfig: function() { /* Promise<Record<string, any>> */ }
};
```

#### 7.1.5 iframe 通信机制

```text
┌─────────────────┐         postMessage         ┌─────────────────┐
│   主框架页面     │  ◄──────────────────────►  │   iframe 页面   │
│  (管理平台壳)    │    origin 校验 + 签名       │  (插件前端页面)  │
└─────────────────┘                           └─────────────────┘
        │                                           │
        │  1. 主框架注入 NexusAdmin Bridge           │
        │  2. iframe 调用 Bridge API                 │
        │  3. Bridge 通过 postMessage 转发到主框架    │
        │  4. 主框架处理并返回结果                   │
```

### 7.2 第二阶段（远期）：Module Federation

远期演进方向为基于 **Webpack Module Federation** 或 **Rspack Module Federation** 的模块联邦方案：

1. **构建期**：插件前端代码打包为远程模块（Remote），暴露指定组件
2. **运行期**：主框架作为 Host 动态加载远程模块
3. **路由渲染**：插件路由组件通过模块联邦动态导入，不再使用 iframe
4. **优势**：无 iframe 边框、共享依赖、更好的用户体验和性能
5. **挑战**：需要统一前端技术栈、构建工具、运行时版本，目前作为远期目标规划

------

## 8. 与后端 API 的协作流程

### 8.1 完整生命周期流程

```text
插件安装 / 启动
      │
      ▼
┌─────────────────┐
│ PluginManager   │
│ 加载 plugin.json│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 解析 contributes │
│ 字段             │
└────────┬────────┘
         │
         ├── 注册菜单 ────────► UIContributionRegistry.menus
         │
         ├── 注册路由 ────────► UIContributionRegistry.routes
         │
         ├── 注册挂载点 ──────► UIContributionRegistry.mountPoints
         │
         └── 注册权限 ────────► PermissionDeclarationRegistry
         │
         ▼
前端请求 GET /admin/v1/ui/manifest
         │
         ▼
┌─────────────────┐
│ 后端聚合所有活跃  │
│ 插件的 contributes│
└────────┬────────┘
         │
         ▼
前端根据 manifest 渲染菜单、注册路由、填充挂载点
```

### 8.2 前端初始化流程

```text
用户登录成功
      │
      ├──► GET /admin/v1/auth/me
      │         └── 获取用户信息和权限列表
      │
      ├──► GET /admin/v1/ui/manifest
      │         └── 获取 UI 贡献声明
      │
      ▼
  权限过滤
      │
      ├── 根据权限列表过滤菜单
      ├── 根据权限列表过滤路由
      └── 根据权限列表过滤挂载点
      │
      ▼
  动态注册路由
      │
      ▼
  渲染菜单树
      │
      ▼
  填充各挂载点内容
      │
      ▼
  用户开始使用管理面板
```

### 8.3 Manifest API

#### 请求

```text
GET /admin/v1/ui/manifest
Authorization: Bearer {token}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "menus": [
      {
        "pluginId": "analytics-plugin",
        "id": "analytics.dashboard",
        "label": "数据分析",
        "icon": "chart-line",
        "path": "/plugins/analytics/dashboard",
        "order": 100,
        "parent": "root",
        "permissions": ["analytics.view"]
      },
      {
        "pluginId": "analytics-plugin",
        "id": "analytics.reports",
        "label": "报表管理",
        "icon": "file-text",
        "path": "/plugins/analytics/reports",
        "order": 110,
        "parent": "analytics.dashboard",
        "permissions": ["analytics.reports.view"]
      }
    ],
    "routes": [
      {
        "pluginId": "analytics-plugin",
        "path": "/plugins/analytics/dashboard",
        "title": "数据分析仪表盘",
        "component": "analytics-plugin/pages/Dashboard",
        "layout": "default",
        "permissions": ["analytics.view"],
        "props": {
          "refreshInterval": 30
        },
        "meta": {
          "breadcrumb": ["数据分析", "仪表盘"],
          "cache": true
        }
      }
    ],
    "mountPoints": [
      {
        "pluginId": "analytics-plugin",
        "slot": "dashboard.widgets",
        "component": "analytics-plugin/components/SummaryWidget",
        "order": 50,
        "permissions": ["analytics.view"],
        "props": {
          "title": "数据概览",
          "size": "medium"
        }
      }
    ],
    "permissions": [
      {
        "pluginId": "analytics-plugin",
        "id": "analytics.view",
        "label": "查看分析数据",
        "description": "允许访问数据分析仪表盘和报表",
        "group": "数据分析"
      },
      {
        "pluginId": "analytics-plugin",
        "id": "analytics.export",
        "label": "导出分析报告",
        "description": "允许导出分析报告为 PDF/Excel",
        "group": "数据分析",
        "dependencies": ["analytics.view"]
      }
    ]
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `menus` | `object[]` | 所有活跃插件贡献的菜单项，已按 `parent` 和 `order` 排序 |
| `routes` | `object[]` | 所有活跃插件贡献的路由定义 |
| `mountPoints` | `object[]` | 所有活跃插件贡献的挂载点内容，已按 `slot` 和 `order` 分组排序 |
| `permissions` | `object[]` | 所有活跃插件声明的权限定义 |

**注意**：`manifest` 响应**不包含**权限过滤后的数据，前端需根据 `GET /admin/v1/auth/me` 返回的权限列表自行过滤。

------

## 9. 完整的插件示例

以下是一个完整的 `plugin.json` 示例，展示了 `contributes` 所有字段的用法：

```json
{
  "id": "analytics-plugin",
  "version": "1.0.0",
  "name": "数据分析插件",
  "description": "提供数据仪表盘、报表管理和数据导出功能",
  "author": "Nexus Team <team@nexusadmin.com>",
  "mainClass": "com.nexusadmin.plugin.analytics.AnalyticsPlugin",
  "coreVersion": "^1.0.0",
  "dependencies": {
    "core-auth": "^1.0.0"
  },
  "contributes": {
    "menus": [
      {
        "id": "analytics.dashboard",
        "label": "数据分析",
        "icon": "chart-line",
        "path": "/plugins/analytics/dashboard",
        "order": 100,
        "parent": "business",
        "permissions": ["analytics.view"],
        "badge": {
          "type": "dot",
          "api": "/api/analytics/unread-count"
        }
      },
      {
        "id": "analytics.reports",
        "label": "报表管理",
        "icon": "file-text",
        "path": "/plugins/analytics/reports",
        "order": 110,
        "parent": "analytics.dashboard",
        "permissions": ["analytics.reports.view"]
      },
      {
        "id": "analytics.settings",
        "label": "分析设置",
        "icon": "settings",
        "path": "/plugins/analytics/settings",
        "order": 120,
        "parent": "analytics.dashboard",
        "permissions": ["analytics.settings.edit"]
      }
    ],
    "routes": [
      {
        "path": "/plugins/analytics/dashboard",
        "title": "数据分析仪表盘",
        "component": "analytics-plugin/pages/Dashboard",
        "layout": "default",
        "permissions": ["analytics.view"],
        "props": {
          "refreshInterval": 30,
          "defaultTimeRange": "7d"
        },
        "meta": {
          "breadcrumb": ["数据分析", "仪表盘"],
          "cache": true
        }
      },
      {
        "path": "/plugins/analytics/reports",
        "title": "报表管理",
        "component": "analytics-plugin/pages/Reports",
        "layout": "default",
        "permissions": ["analytics.reports.view"],
        "meta": {
          "breadcrumb": ["数据分析", "报表管理"],
          "cache": false
        }
      },
      {
        "path": "/plugins/analytics/settings",
        "title": "分析设置",
        "component": "analytics-plugin/pages/Settings",
        "layout": "default",
        "permissions": ["analytics.settings.edit"]
      }
    ],
    "mountPoints": [
      {
        "slot": "dashboard.widgets",
        "component": "analytics-plugin/components/SummaryWidget",
        "order": 50,
        "permissions": ["analytics.view"],
        "props": {
          "title": "数据概览",
          "size": "medium"
        }
      },
      {
        "slot": "dashboard.quickActions",
        "component": "analytics-plugin/components/QuickExport",
        "order": 100,
        "permissions": ["analytics.export"],
        "props": {
          "label": "导出周报",
          "action": "export-weekly"
        }
      },
      {
        "slot": "plugin.detail.tabs",
        "component": "analytics-plugin/components/UsageStatsTab",
        "order": 200,
        "permissions": ["analytics.view"],
        "props": {
          "tabTitle": "使用统计"
        }
      }
    ],
    "permissions": [
      {
        "id": "analytics.view",
        "label": "查看分析数据",
        "description": "允许访问数据分析仪表盘和查看基础报表",
        "group": "数据分析"
      },
      {
        "id": "analytics.reports.view",
        "label": "查看报表",
        "description": "允许查看和筛选报表数据",
        "group": "数据分析",
        "dependencies": ["analytics.view"]
      },
      {
        "id": "analytics.export",
        "label": "导出分析报告",
        "description": "允许导出分析报告为 PDF/Excel 格式",
        "group": "数据分析",
        "dependencies": ["analytics.view"]
      },
      {
        "id": "analytics.settings.edit",
        "label": "编辑分析设置",
        "description": "允许修改数据分析相关的配置参数",
        "group": "数据分析"
      }
    ]
  }
}
```

------

## 10. 总结

插件 UI 贡献机制通过声明式的 `contributes` 字段，让插件能够以零前端代码的方式扩展管理面板的 UI 能力：

1. **声明式配置**：插件通过 `plugin.json` 中的 `contributes` 字段声明菜单、路由、挂载点和权限
2. **静态可分析**：`contributes` 在插件加载阶段即解析，无需等待插件启动完成
3. **iframe 沙箱**：第一阶段采用 iframe + JS Bridge SDK 方案，提供最强的隔离性和安全性
4. **权限联动**：UI 贡献与权限系统深度集成，无权限时自动隐藏或拦截
5. **自动清理**：插件停止/卸载时，其所有 UI 贡献自动移除
6. **演进能力**：架构支持从 iframe 方案向 Module Federation 等高级方案平滑演进

该机制与 Web 接入机制（后端 API 动态注册）共同构成 Nexus Admin 插件化架构的完整 Web 能力扩展体系。
