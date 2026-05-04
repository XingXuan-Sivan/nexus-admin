# ADR-006: Facade 体系重构

---

## 背景与问题

### 1. 已有决策回顾

ADR-004 决定采用 Builder 模式的 `CoreRuntime` 聚合根在 core 模块完成组件装配，在 api 模块保留薄 Spring 桥接层（`CoreAutoConfig`）。

ADR-004 的核心决策：


| 决策项       | 内容                                                           |
| ------------ | -------------------------------------------------------------- |
| 装配方式     | `CoreRuntime.defaults()` 一行获得全部核心组件                  |
| Builder 覆盖 | `CoreRuntime.builder().eventBus(...).build()` 选择性覆盖       |
| Spring 桥接  | `CoreAutoConfig` 委托 CoreRuntime 暴露 Bean                    |
| 覆盖优先级   | app @Bean > api @ConditionalOnMissingBean > CoreRuntime 默认值 |

### 2. ADR-004 决策的不足

经过实际使用和架构演进，该决策暴露出三个问题：

**问题一：CoreRuntime 单一聚合根职责过重**

`CoreRuntime` 将事件总线、扩展注册中心、配置中心、插件子系统全部组件塞进一个聚合根。随着每个子系统自身组件数量增长（如配置中心包含 ConfigStore、SchemaRegistry、ConfigResolver、ConfigUIBuilder 等），CoreRuntime 的 Builder 参数膨胀，构造逻辑日趋复杂。单一聚合根无法反映各子系统独立的演进节奏。

**问题二：硬编码 SPI 注册散落各处**

`DefaultConfigManager` 构造函数中硬编码注册 `JsonPluginSchemaProvider`、`PlatformSchemaProvider`、`JsonSchemaValidator` 等 SPI 实现。`DefaultPluginManager` 的 `initializeConfigComponents()` 中硬编码创建 `FileConfigStore`、`ConfigResolver` 并注册默认 `ConfigSource`。这些硬编码无法被替换，违反了 ADR-003 建立的"SPI 热替换"原则。

**问题三：PluginManager 职责混杂**

`AbstractPluginManager` 直接持有 EventBus、ExtensionRegistry、PluginRegistry、ConfigManager 等底层组件引用，同时承担生命周期编排和组件管理两种职责。构造函数参数多达十几个，且任何内部实现变更都会传导到构造签名。

### 3. 根本原因


| 问题                   | 根因                                                                                                     |
| ---------------------- | -------------------------------------------------------------------------------------------------------- |
| CoreRuntime 单一聚合根 | 四个子系统（事件、扩展、配置、插件）各自有独立的组件图和依赖关系，不应共享一个 Builder                   |
| 硬编码 SPI             | ConfigManager 和 PluginManager 的初始化逻辑中直接 new 具体实现类，而非通过 Runtime 的 Builder 暴露覆盖点 |
| PluginManager 职责混杂 | 缺乏 Facade 层隔离，PluginManager 直接操作底层组件而非通过门面接口                                       |

---

## 决策方案

采用 **Runtime 拆分 + Facade 隔离 + CoreConfig 封装** 三层重构：

1. **Runtime 拆分**：将 CoreRuntime 拆分为 4 个独立 Runtime（EventBusRuntime、ExtensionRuntime、ConfigRuntime、PluginRuntime），每个 Runtime 负责自己子系统的组件装配
2. **Facade 隔离**：引入 4 个 Facade（EventBusFacade、ExtensionFacade、ConfigFacade、PluginFacade），作为 PluginManager 访问底层组件的唯一入口
3. **CoreConfig 封装**：引入 CoreConfig 封装运行时配置（RuntimeMode、pluginsDataRoot），替代 PluginManager 构造函数中的配置参数

### 总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        nexus-admin-core                         │
│  (零框架依赖：slf4j、minimal-json、snakeyaml 仅此而已)            │
│                                                                 │
│  ┌──────────────────┐ ┌──────────────────┐                     │
│  │ EventBusRuntime  │ │ ExtensionRuntime │  各自 Builder       │
│  │  └ EventBusFacade│ │  └ ExtensionFacade│  装配 + 门面       │
│  └──────────────────┘ └──────────────────┘                     │
│  ┌──────────────────┐ ┌──────────────────┐                     │
│  │ ConfigRuntime    │ │ PluginRuntime    │  硬编码消除         │
│  │  └ ConfigFacade  │ │  └ PluginFacade  │  SPI 可覆盖         │
│  └──────────────────┘ └──────────────────┘                     │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ CoreConfig (运行时配置: RuntimeMode + pluginsDataRoot)     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ AbstractPluginManager                                    │   │
│  │   持有: CoreConfig + 4 个 Facade                         │   │
│  │   不再直接持有底层组件引用                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────────┘
                         │ 依赖
┌────────────────────────▼────────────────────────────────────────┐
│                      nexus-admin-api                             │
│  (引入 Spring Web，提供 REST 与管理面板)                          │
│                                                                 │
│  ┌──────────────────┐ ┌──────────────────┐                     │
│  │ EventBusAutoConfig│ │ ExtensionAutoConfig│  4 个独立        │
│  └──────────────────┘ └──────────────────┘  桥接配置          │
│  ┌──────────────────┐ ┌──────────────────┐                     │
│  │ ConfigAutoConfig  │ │ PluginAutoConfig  │  @ConditionalOn.. │
│  └──────────────────┘ └──────────────────┘  MissingBean 保护   │
└─────────────────────────────────────────────────────────────────┘
```

### 四大 Runtime 设计

#### EventBusRuntime

```java
// 路径: core/runtime/EventBusRuntime.java
public final class EventBusRuntime {
    private final EventBus eventBus;

    // 全默认装配
    public static EventBusRuntime defaults() { ... }

    // 选择性覆盖
    public static Builder builder() { ... }

    // 组件访问
    public EventBus eventBus() { ... }
    public EventBusFacade facade() { ... }
}
```

**组件清单**：


| 组件     | 默认实现     | 可通过 Builder 覆盖 |
| -------- | ------------ | ------------------- |
| EventBus | SyncEventBus | ✅                  |

#### ExtensionRuntime

```java
// 路径: core/runtime/ExtensionRuntime.java
public final class ExtensionRuntime {
    private final EventBus eventBus;
    private final ExtensionRegistry extensionRegistry;

    // 全默认装配（内部创建 SyncEventBus）
    public static ExtensionRuntime defaults() { ... }

    // 注入外部 EventBus（推荐，与其他运行时共享同一总线）
    public static Builder builder() { ... }

    // 组件访问
    public EventBus eventBus() { ... }
    public ExtensionRegistry extensionRegistry() { ... }
    public ExtensionFacade facade() { ... }
}
```

**组件清单**：


| 组件              | 默认实现                           | 可通过 Builder 覆盖 |
| ----------------- | ---------------------------------- | ------------------- |
| EventBus          | SyncEventBus                       | ✅                  |
| ExtensionRegistry | DefaultExtensionRegistry(eventBus) | ✅                  |

#### ConfigRuntime

```java
// 路径: core/runtime/ConfigRuntime.java
public final class ConfigRuntime {
    private final ConfigManager configManager;
    private final SchemaRegistry schemaRegistry;
    private final ConfigResolver configResolver;
    private final ConfigStore configStore;
    private final ConfigUIBuilder uiBuilder;
    private final EventBus eventBus;
    private final Path configDir;

    // 必须提供 configDir 和 eventBus
    public static Builder builder() { ... }

    // 组件访问
    public ConfigManager configManager() { ... }
    public ConfigFacade facade() { ... }
    // ...其他访问器
}
```

**组件清单**：


| 组件                 | 默认实现                                                 | 可通过 Builder 覆盖      |
| -------------------- | -------------------------------------------------------- | ------------------------ |
| ConfigStore          | FileConfigStore(configDir)                               | ✅                       |
| SchemaRegistry       | new SchemaRegistry()                                     | ✅                       |
| ConfigResolver       | new ConfigResolver() + 默认 ConfigSource                 | ✅                       |
| ConfigUIBuilder      | new ConfigUIBuilder(schemaRegistry)                      | ✅                       |
| ConfigManager        | new DefaultConfigManager(...)                            | ✅（通过覆盖子组件间接） |
| SchemaProvider 列表  | JsonPluginSchemaProvider + PlatformSchemaProvider        | ✅                       |
| SchemaValidator 列表 | JsonSchemaValidator                                      | ✅                       |
| ConfigSource 列表    | EnvConfigSource + FileConfigSource + DefaultConfigSource | ✅                       |

**硬编码消除**：原 `DefaultConfigManager` 构造函数中硬编码注册的 SchemaProvider、SchemaValidator、ConfigSource，现全部由 ConfigRuntime.Builder 管理，可选择性覆盖。

#### PluginRuntime

```java
// 路径: core/runtime/PluginRuntime.java
public final class PluginRuntime {
    private final PluginRegistry pluginRegistry;
    private final VersionManager versionManager;
    private final DependenceManager dependenceManager;
    private final PluginLoader pluginLoader;
    private final List<PluginSource> sources;
    private final List<PluginDescriptorFinder> finders;
    private final List<PluginDescriptorParser> parsers;

    // 全默认装配
    public static PluginRuntime defaults() { ... }

    // 选择性覆盖 + 注入插件源
    public static Builder builder() { ... }

    // 组件访问
    public PluginRegistry pluginRegistry() { ... }
    public PluginFacade facade() { ... }
    // ...其他访问器
}
```

**组件清单**：


| 组件                        | 默认实现                                 | 可通过 Builder 覆盖 |
| --------------------------- | ---------------------------------------- | ------------------- |
| PluginRegistry              | DefaultPluginRegistry                    | ✅                  |
| VersionManager              | DefaultVersionManager                    | ✅                  |
| DependenceManager           | DefaultDependenceManager(versionManager) | ✅                  |
| PluginLoader                | DefaultPluginLoader                      | ✅                  |
| PluginSource 列表           | 空列表（由外部注入）                     | ✅                  |
| PluginDescriptorFinder 列表 | 空列表（由外部注入）                     | ✅                  |
| PluginDescriptorParser 列表 | 空列表（由外部注入）                     | ✅                  |

### 四大 Facade 设计

Facade 是 PluginManager 访问底层组件的唯一入口，职责边界清晰：


| Facade          | 聚合组件                                                                                     | 核心方法                                                    |
| --------------- | -------------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| EventBusFacade  | EventBus                                                                                     | publish / subscribe / unsubscribe                           |
| ExtensionFacade | ExtensionRegistry                                                                            | register / getFirst / getExtensions / unregisterPlugin      |
| ConfigFacade    | ConfigManager                                                                                | get / set / getSchema / registerPlugin / isPluginDisabled   |
| PluginFacade    | PluginRegistry + PluginLoader + VersionManager + DependenceManager + sources/finders/parsers | getPlugin / listPlugins / createDiscoverer / createResolver |

**设计原则**：

- Facade 不创建组件，仅聚合已有实例
- Facade 方法名面向业务语义，不暴露底层 API 细节
- Facade 提供 `xxxRegistry()` / `eventBus()` 等方法供需要直接访问底层接口的场景使用

### CoreConfig 设计

```java
// 路径: core/CoreConfig.java
public class CoreConfig {
    private final RuntimeMode runtimeMode;
    private final Path pluginsDataRoot;

    public static CoreConfig of(RuntimeMode runtimeMode, Path pluginsDataRoot) { ... }

    public RuntimeMode runtimeMode() { ... }
    public Path pluginsDataRoot() { ... }
}
```

**设计要点**：

- 不可变值对象，替代 PluginManager 构造函数中散落的 `RuntimeMode` 和 `Path` 参数
- 避免参数膨胀，未来新增运行时配置项只需扩展 CoreConfig

### AbstractPluginManager / DefaultPluginManager 重构

**重构前**：

```java
// AbstractPluginManager 直接持有底层组件引用
public abstract class AbstractPluginManager implements PluginManager {
    protected final PluginRegistry pluginRegistry;
    protected final ExtensionRegistry extensionRegistry;
    protected final EventBus eventBus;
    protected final ConfigManager configManager;
    // ...多个子组件字段
}
```

**重构后**：

```java
// AbstractPluginManager 仅持有 CoreConfig + 4 个 Facade
public abstract class AbstractPluginManager implements PluginManager {
    protected final CoreConfig coreConfig;
    protected final PluginFacade pluginFacade;
    protected final ExtensionFacade extensionFacade;
    protected final ConfigFacade configFacade;
    protected final EventBusFacade eventBusFacade;
}
```

**构造函数对比**：


|                                | 重构前         | 重构后                   |
| ------------------------------ | -------------- | ------------------------ |
| AbstractPluginManager 构造参数 | 10+ 个底层组件 | CoreConfig + 4 个 Facade |
| DefaultPluginManager 构造参数  | 10+ 个底层组件 | CoreConfig + 4 个 Facade |

### Spring 桥接层设计（api 模块）

原来的单一 `CoreAutoConfig` 拆分为 4 个独立桥接配置：


| 桥接配置            | 对应 Runtime     | 暴露的 Bean                                                                                  |
| ------------------- | ---------------- | -------------------------------------------------------------------------------------------- |
| EventBusAutoConfig  | EventBusRuntime  | EventBusRuntime, EventBus, EventBusFacade                                                    |
| ExtensionAutoConfig | ExtensionRuntime | ExtensionRuntime, ExtensionRegistry, ExtensionFacade                                         |
| ConfigAutoConfig    | ConfigRuntime    | ConfigRuntime, ConfigManager, ConfigFacade                                                   |
| PluginAutoConfig    | PluginRuntime    | PluginRuntime, PluginRegistry, PluginFacade, VersionManager, DependenceManager, PluginLoader |

**桥接层职责边界**（与 ADR-004 一致）：

- ✅ 将 Runtime 暴露为 Spring Bean
- ✅ 将各组件通过访问器委托暴露为独立 Bean（保持向后兼容）
- ✅ 声明 `@ConditionalOnMissingBean` 保留覆盖能力
- ❌ 不包含任何组件创建逻辑
- ❌ 不包含依赖注入逻辑

### 覆盖机制

```
优先级：app @Bean > api AutoConfig @ConditionalOnMissingBean > Runtime.builder() > Runtime 默认值
```

**场景 1：覆盖单个组件（Spring 环境）**

```java
// 在 app 模块中声明同类型 Bean 即可覆盖
@Bean
public EventBus eventBus() {
    return new AsyncEventBus();  // 替换默认的 SyncEventBus
}
```

**场景 2：覆盖某个子系统的全部组件（Spring 环境）**

```java
// 声明对应 Runtime Bean 即可级联替换该子系统全部组件
@Bean
public EventBusRuntime eventBusRuntime() {
    return EventBusRuntime.builder()
            .eventBus(new AsyncEventBus())
            .build();
}
```

**场景 3：非 Spring 环境直接使用**

```java
// 分别创建各子系统的 Runtime
EventBusRuntime eventBusRt = EventBusRuntime.defaults();
ExtensionRuntime extensionRt = ExtensionRuntime.builder()
        .eventBus(eventBusRt.eventBus())
        .build();
ConfigRuntime configRt = ConfigRuntime.builder()
        .configDir(Path.of("plugins-data/config"))
        .eventBus(eventBusRt.eventBus())
        .build();
PluginRuntime pluginRt = PluginRuntime.defaults();

// 组装 PluginManager
CoreConfig coreConfig = CoreConfig.of(RuntimeMode.DEV, Path.of("plugins-data"));
PluginManager pm = new DefaultPluginManager(
        coreConfig,
        pluginRt.facade(),
        extensionRt.facade(),
        configRt.facade(),
        eventBusRt.facade()
);
```

---

## 备选方案对比


| 维度               | A. Runtime 拆分 + Facade<br/>⭐ 推荐  | B. 保持 CoreRuntime + 增加 Facade               | C. 保持现状（ADR-004）                                   |
| ------------------ | ------------------------------------- | ----------------------------------------------- | -------------------------------------------------------- |
| 子系统独立演进     | ✅ 各 Runtime 独立 Builder、独立版本  | ⚠️ 共享 CoreRuntime Builder，参数持续膨胀     | ❌ 单一聚合根耦合所有子系统                              |
| 硬编码 SPI 消除    | ✅ ConfigRuntime.Builder 管理全部 SPI | ⚠️ 需在 CoreRuntime.Builder 中增加 SPI 覆盖点 | ❌ 硬编码在 DefaultConfigManager/DefaultPluginManager 中 |
| PluginManager 职责 | ✅ 仅持有 CoreConfig + 4 Facade       | ⚠️ 仍直接持有底层组件                         | ❌ 持有 10+ 底层组件引用                                 |
| Spring 桥接粒度    | ✅ 4 个独立 AutoConfig，按需覆盖      | ⚠️ 单一 CoreAutoConfig                        | ⚠️ 单一 CoreAutoConfig                                 |
| 改动规模           | 中（新增 8 文件 + 重构 4 文件）       | 中（新增 4 Facade + 重构 2 文件）               | 无                                                       |
| 与 ADR-004 一致性  | ✅ 继承 Builder 装配理念，粒度更细    | ✅ 继承 Builder 装配理念                        | —                                                       |
| 学习成本           | 低（每个 Runtime 结构一致，模式统一） | 低                                              | 低                                                       |

**选择 A 的理由总结**：

1. **子系统独立演进**——每个 Runtime 职责单一，Builder 参数有限且稳定
2. **硬编码 SPI 彻底消除**——ConfigRuntime.Builder 管理全部 SPI 注册，可选择性覆盖
3. **PluginManager 职责回归**——仅持有 CoreConfig + 4 个 Facade，构造签名简洁稳定
4. **Spring 桥接粒度更细**——4 个独立 AutoConfig，可按子系统覆盖
5. **模式统一**——4 个 Runtime 和 4 个 Facade 遵循相同的设计模式，学习成本低

---

## 覆盖 ADR-004 的决策


| ADR-004 决策                      | 新决策                                       | 变更原因                                       |
| --------------------------------- | -------------------------------------------- | ---------------------------------------------- |
| CoreRuntime 单一聚合根            | 拆分为 4 个独立 Runtime                      | 子系统独立演进，Builder 参数不再膨胀           |
| CoreAutoConfig 单一桥接           | 拆分为 4 个独立 AutoConfig                   | 桥接粒度更细，按子系统覆盖                     |
| PluginManager 直接持有底层组件    | 通过 4 个 Facade 间接访问                    | 职责隔离，构造签名简化                         |
| RuntimeMode/Path 散落在构造参数中 | CoreConfig 封装运行时配置                    | 参数归拢，可扩展                               |
| 覆盖优先级 3 级                   | 覆盖优先级 4 级（增加 Runtime.builder() 级） | Builder 选择性覆盖介于 Spring 覆盖和默认值之间 |

保留不变的部分：

- Builder 模式装配核心思想
- `@ConditionalOnMissingBean` 覆盖机制
- 零框架依赖原则（core 模块）
- 薄 Spring 桥接层理念

---

## 实施计划

### 阶段 1：core 模块新增 4 个 Runtime + 4 个 Facade + CoreConfig


| 步骤 | 操作                        | 文件                                 |
| ---- | --------------------------- | ------------------------------------ |
| 1.1  | 新增`EventBusRuntime.java`  | `core/runtime/EventBusRuntime.java`  |
| 1.2  | 新增`ExtensionRuntime.java` | `core/runtime/ExtensionRuntime.java` |
| 1.3  | 新增`ConfigRuntime.java`    | `core/runtime/ConfigRuntime.java`    |
| 1.4  | 新增`PluginRuntime.java`    | `core/runtime/PluginRuntime.java`    |
| 1.5  | 新增`EventBusFacade.java`   | `core/facade/EventBusFacade.java`    |
| 1.6  | 新增`ExtensionFacade.java`  | `core/facade/ExtensionFacade.java`   |
| 1.7  | 新增`ConfigFacade.java`     | `core/facade/ConfigFacade.java`      |
| 1.8  | 新增`PluginFacade.java`     | `core/facade/PluginFacade.java`      |
| 1.9  | 新增`CoreConfig.java`       | `core/CoreConfig.java`               |

### 阶段 2：core 模块重构 AbstractPluginManager / DefaultPluginManager / DefaultConfigManager


| 步骤 | 操作                                                                      | 文件                                    |
| ---- | ------------------------------------------------------------------------- | --------------------------------------- |
| 2.1  | 重构`AbstractPluginManager`：移除底层组件字段，改为 CoreConfig + 4 Facade | `core/AbstractPluginManager.java`       |
| 2.2  | 重构`DefaultPluginManager`：精简构造参数                                  | `core/DefaultPluginManager.java`        |
| 2.3  | 重构`DefaultConfigManager`：移除构造函数硬编码注册                        | `core/config/DefaultConfigManager.java` |
| 2.4  | 删除`CoreRuntime.java`                                                    | `core/runtime/CoreRuntime.java`         |

### 阶段 3：api 模块重构 CoreAutoConfig → 4 个独立 AutoConfig


| 步骤 | 操作                           | 文件                                  |
| ---- | ------------------------------ | ------------------------------------- |
| 3.1  | 新增`EventBusAutoConfig.java`  | `api/config/EventBusAutoConfig.java`  |
| 3.2  | 新增`ExtensionAutoConfig.java` | `api/config/ExtensionAutoConfig.java` |
| 3.3  | 新增`ConfigAutoConfig.java`    | `api/config/ConfigAutoConfig.java`    |
| 3.4  | 新增`PluginAutoConfig.java`    | `api/config/PluginAutoConfig.java`    |
| 3.5  | 删除`CoreAutoConfig.java`      | `api/config/CoreAutoConfig.java`      |

### 阶段 4：验证


| 步骤 | 操作                                                      |
| ---- | --------------------------------------------------------- |
| 4.1  | 验证`Application.java` 正常启动                           |
| 4.2  | 验证`BootstrapConfig` 仍然能注入所有 Bean                 |
| 4.3  | 验证各 AutoConfig 的覆盖机制正确                          |
| 4.4  | 验证非 Spring 环境下独立创建 Runtime 并组装 PluginManager |

### 阶段 5：文档同步


| 步骤 | 操作                           | 文件                            |
| ---- | ------------------------------ | ------------------------------- |
| 5.1  | 新增 ADR-006                   | `决策记录/ADR-006-...`          |
| 5.2  | 更新整体架构文档               | `设计文档/整体架构.md`          |
| 5.3  | 更新配置中心文档               | `设计文档/配置中心.md`          |
| 5.4  | 更新插件系统文档               | `设计文档/插件系统.md`          |
| 5.5  | 更新拓展点系统文档             | `设计文档/拓展点系统.md`        |
| 5.6  | 更新事件系统文档               | `设计文档/事件系统.md`          |
| 5.7  | 更新独立使用 Core 模块用户指南 | `user-guid/独立使用Core模块.md` |

---

## 影响范围

### 模块影响


| 模块             | 影响                                        | 风险                         |
| ---------------- | ------------------------------------------- | ---------------------------- |
| nexus-admin-core | 新增 9 个文件，重构 3 个文件，删除 1 个文件 | 低（新增为主，重构逻辑等价） |
| nexus-admin-api  | 新增 4 个文件，删除 1 个文件                | 低（桥接层语义不变）         |
| nexus-admin-app  | 修改 BootstrapConfig 适配新构造签名         | 低（参数来源不变）           |
| plugins          | 零修改                                      | 无                           |

### 文件变更清单


| 文件                                    | 变更类型 | 说明                           |
| --------------------------------------- | -------- | ------------------------------ |
| `core/runtime/EventBusRuntime.java`     | **新增** | 事件总线运行时                 |
| `core/runtime/ExtensionRuntime.java`    | **新增** | 扩展注册中心运行时             |
| `core/runtime/ConfigRuntime.java`       | **新增** | 配置中心运行时                 |
| `core/runtime/PluginRuntime.java`       | **新增** | 插件运行时                     |
| `core/facade/EventBusFacade.java`       | **新增** | 事件总线门面                   |
| `core/facade/ExtensionFacade.java`      | **新增** | 扩展注册中心门面               |
| `core/facade/ConfigFacade.java`         | **新增** | 配置管理门面                   |
| `core/facade/PluginFacade.java`         | **新增** | 插件组件门面                   |
| `core/CoreConfig.java`                  | **新增** | 核心运行时配置                 |
| `core/AbstractPluginManager.java`       | **修改** | 改为持有 CoreConfig + 4 Facade |
| `core/DefaultPluginManager.java`        | **修改** | 精简构造参数                   |
| `core/config/DefaultConfigManager.java` | **修改** | 移除构造函数硬编码注册         |
| `core/runtime/CoreRuntime.java`         | **删除** | 由 4 个独立 Runtime 替代       |
| `api/config/EventBusAutoConfig.java`    | **新增** | 事件总线 Spring 桥接           |
| `api/config/ExtensionAutoConfig.java`   | **新增** | 扩展注册中心 Spring 桥接       |
| `api/config/ConfigAutoConfig.java`      | **新增** | 配置中心 Spring 桥接           |
| `api/config/PluginAutoConfig.java`      | **新增** | 插件子系统 Spring 桥接         |
| `api/config/CoreAutoConfig.java`        | **删除** | 由 4 个独立 AutoConfig 替代    |

### 向后兼容性


| 维度                      | 兼容性        | 说明                                                                 |
| ------------------------- | ------------- | -------------------------------------------------------------------- |
| Bean 名称                 | ⚠️ 部分变更 | 原 CoreRuntime Bean 拆分为 4 个 Runtime Bean；底层组件 Bean 名称不变 |
| Bean 类型                 | ⚠️ 部分变更 | CoreRuntime 类型删除，新增 4 个 Runtime + 4 个 Facade 类型           |
| @ConditionalOnMissingBean | ✅ 保留       | 覆盖机制不变                                                         |
| PluginManager 构造签名    | ❌ 不兼容     | 从 10+ 参数改为 CoreConfig + 4 Facade（内部使用，影响可控）          |
| 外部依赖 core 的项目      | ⚠️ 需适配   | CoreRuntime.defaults() 替换为各 Runtime 分别创建                     |
| 外部依赖 api 的项目       | ✅ 完全兼容   | 底层组件 Bean 注入方式不变                                           |

---

## 风险与缓解


| 风险                                              | 影响                   | 可能性 | 缓解措施                                                |
| ------------------------------------------------- | ---------------------- | ------ | ------------------------------------------------------- |
| PluginManager 构造签名变更导致编译错误            | app 模块需适配         | 高     | 构造签名简化后更清晰，适配工作量小                      |
| CoreRuntime 删除影响外部项目                      | 非 Spring 使用者需修改 | 中     | ADR-006 文档明确迁移路径；新 API 更清晰                 |
| 4 个 AutoConfig 间 Bean 依赖顺序                  | Spring 容器启动失败    | 低     | 各 AutoConfig 通过方法参数声明依赖，Spring 自动解析顺序 |
| ConfigRuntime 必填字段（eventBus、configDir）遗漏 | 构建失败               | 低     | Builder.build() 中 Objects.requireNonNull 防御          |

---

## 总结

**核心决策**：

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│   4 个 Runtime → core 模块                              │
│   ├─ EventBusRuntime   (事件总线装配)                    │
│   ├─ ExtensionRuntime  (扩展注册中心装配)                 │
│   ├─ ConfigRuntime     (配置中心装配，硬编码 SPI 消除)     │
│   └─ PluginRuntime     (插件子系统装配)                   │
│                                                        │
│   4 个 Facade → core 模块                               │
│   ├─ EventBusFacade    (事件总线门面)                     │
│   ├─ ExtensionFacade   (扩展注册中心门面)                 │
│   ├─ ConfigFacade      (配置管理门面)                     │
│   └─ PluginFacade      (插件组件门面)                     │
│                                                        │
│   CoreConfig → core 模块                               │
│   └─ RuntimeMode + pluginsDataRoot 封装                 │
│                                                        │
│   4 个 AutoConfig → api 模块                            │
│   ├─ EventBusAutoConfig (桥接 EventBusRuntime)          │
│   ├─ ExtensionAutoConfig (桥接 ExtensionRuntime)        │
│   ├─ ConfigAutoConfig   (桥接 ConfigRuntime)            │
│   └─ PluginAutoConfig   (桥接 PluginRuntime)            │
│                                                        │
│   删除：CoreRuntime、CoreAutoConfig                     │
│                                                        │
└────────────────────────────────────────────────────────┘
```

**关键收益**：

1. 子系统独立演进，Builder 参数不再膨胀
2. 硬编码 SPI 彻底消除，ConfigRuntime.Builder 管理全部可覆盖点
3. PluginManager 职责回归，仅持有 CoreConfig + 4 个 Facade
4. Spring 桥接粒度更细，4 个独立 AutoConfig 可按子系统覆盖
5. 模式统一，4 个 Runtime 和 4 个 Facade 遵循相同设计模式
