# 独立使用 Core 模块

---

## 概述

`nexus-admin-core` 是 Nexus Admin 的微内核运行时模块，可脱离 Spring 框架独立使用。
通过 4 个独立 Runtime 分别获得各子系统的默认装配，也可通过 Builder 选择性覆盖。

---

## 依赖引入

**Maven**：

```xml
<dependency>
    <groupId>com.nexusadmin</groupId>
    <artifactId>nexus-admin-core</artifactId>
    <version>${nexus-admin.version}</version>
</dependency>
```

Core 模块仅依赖 slf4j、minimal-json、snakeyaml，不依赖任何技术框架。

---

## 非 Spring 环境使用

### 全默认装配

分别创建各子系统的 Runtime：

```java
// 1. 事件总线（无外部依赖，可全默认）
EventBusRuntime eventBusRt = EventBusRuntime.defaults();

// 2. 扩展注册中心（依赖 EventBus，注入共享总线）
ExtensionRuntime extensionRt = ExtensionRuntime.builder()
        .eventBus(eventBusRt.eventBus())
        .build();

// 3. 配置中心（依赖 EventBus + configDir）
ConfigRuntime configRt = ConfigRuntime.builder()
        .configDir(Path.of("plugins-data/config"))
        .eventBus(eventBusRt.eventBus())
        .build();

// 4. 插件子系统（无外部依赖，可全默认）
PluginRuntime pluginRt = PluginRuntime.defaults();
```

### 选择性覆盖

每个 Runtime 均支持 Builder 选择性覆盖，未指定的组件使用默认实现：

```java
// 替换默认的同步事件总线
EventBusRuntime eventBusRt = EventBusRuntime.builder()
        .eventBus(new MyAsyncEventBus())
        .build();

// 替换扩展注册中心实现
ExtensionRuntime extensionRt = ExtensionRuntime.builder()
        .eventBus(eventBusRt.eventBus())
        .extensionRegistry(new CustomExtensionRegistry())
        .build();

// 覆盖配置中心的 SchemaProvider
ConfigRuntime configRt = ConfigRuntime.builder()
        .configDir(Path.of("plugins-data/config"))
        .eventBus(eventBusRt.eventBus())
        .schemaProviders(List.of(myCustomProvider))
        .build();
```

### 与 PluginManager 集成

```java
// 创建各子系统 Runtime
EventBusRuntime eventBusRt = EventBusRuntime.defaults();
ExtensionRuntime extensionRt = ExtensionRuntime.builder()
        .eventBus(eventBusRt.eventBus())
        .build();
ConfigRuntime configRt = ConfigRuntime.builder()
        .configDir(Path.of("plugins-data/config"))
        .eventBus(eventBusRt.eventBus())
        .build();
PluginRuntime pluginRt = PluginRuntime.defaults();

// 组装 CoreConfig
CoreConfig coreConfig = CoreConfig.of(RuntimeMode.DEV, Path.of("plugins-data"));

// 创建 PluginManager
PluginManager pluginManager = new DefaultPluginManager(
        coreConfig,
        pluginRt.facade(),
        extensionRt.facade(),
        configRt.facade(),
        eventBusRt.facade()
);
```

---

## Spring 环境使用

### 全默认（开箱即用）

引入 `nexus-admin-api` 模块后，4 个 AutoConfig 自动将核心组件桥接到 Spring 容器。
无需额外配置即可通过 `@Autowired` 注入：

```java
@Autowired
private EventBus eventBus;

@Autowired
private ExtensionRegistry extensionRegistry;

@Autowired
private EventBusFacade eventBusFacade;

@Autowired
private ConfigFacade configFacade;
```

### 覆盖单个组件

声明同类型 Bean 即可覆盖默认值：

```java
@Configuration
public class MyConfig {

    @Bean
    public EventBus eventBus() {
        // Spring 容器将使用此 Bean 而非 Runtime 中的默认值
        return new AsyncEventBus();
    }
}
```

### 覆盖某个子系统的全部组件

声明对应 Runtime Bean 可级联替换该子系统全部组件：

```java
@Configuration
public class MyConfig {

    @Bean
    public EventBusRuntime eventBusRuntime() {
        return EventBusRuntime.builder()
                .eventBus(new AsyncEventBus())
                .build();
    }

    @Bean
    public ConfigRuntime configRuntime(EventBus eventBus) {
        return ConfigRuntime.builder()
                .eventBus(eventBus)
                .configDir(Path.of("custom-config"))
                .build();
    }
}
```

### 不引入 api 模块的手动桥接

若仅依赖 `nexus-admin-core` 而未引入 `nexus-admin-api`，
需要自行编写 Spring 配置类将各 Runtime 桥接到容器：

**Maven 依赖**（仅 core + Spring）：

```xml
<dependency>
    <groupId>com.nexusadmin</groupId>
    <artifactId>nexus-admin-core</artifactId>
    <version>${nexus-admin.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

**手动桥接配置**：

```java
import com.nexusadmin.core.CoreConfig;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.facade.ConfigFacade;
import com.nexusadmin.core.facade.EventBusFacade;
import com.nexusadmin.core.facade.ExtensionFacade;
import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import com.nexusadmin.core.runtime.ConfigRuntime;
import com.nexusadmin.core.runtime.EventBusRuntime;
import com.nexusadmin.core.runtime.ExtensionRuntime;
import com.nexusadmin.core.runtime.PluginRuntime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将各 Runtime 桥接到 Spring 容器的手动配置。
 * <p>职责与 api 模块中的 4 个 AutoConfig 相同，由使用者自行维护。</p>
 */
@Configuration
public class CoreRuntimeBridgeConfig {

    // ---- EventBusRuntime ----

    @Bean
    @ConditionalOnMissingBean
    public EventBusRuntime eventBusRuntime() {
        return EventBusRuntime.defaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus(EventBusRuntime rt) {
        return rt.eventBus();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBusFacade eventBusFacade(EventBusRuntime rt) {
        return rt.facade();
    }

    // ---- ExtensionRuntime ----

    @Bean
    @ConditionalOnMissingBean
    public ExtensionRuntime extensionRuntime(EventBus eventBus) {
        return ExtensionRuntime.builder().eventBus(eventBus).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry(ExtensionRuntime rt) {
        return rt.extensionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExtensionFacade extensionFacade(ExtensionRuntime rt) {
        return rt.facade();
    }

    // ---- ConfigRuntime ----

    @Bean
    @ConditionalOnMissingBean
    public ConfigRuntime configRuntime(EventBus eventBus) {
        return ConfigRuntime.builder()
                .eventBus(eventBus)
                .configDir(Path.of("plugins-data/config"))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigFacade configFacade(ConfigRuntime rt) {
        return rt.facade();
    }

    // ---- PluginRuntime ----

    @Bean
    @ConditionalOnMissingBean
    public PluginRuntime pluginRuntime() {
        return PluginRuntime.defaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry(PluginRuntime rt) {
        return rt.pluginRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginFacade pluginFacade(PluginRuntime rt) {
        return rt.facade();
    }

    @Bean
    @ConditionalOnMissingBean
    public VersionManager versionManager(PluginRuntime rt) {
        return rt.versionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public DependenceManager dependenceManager(PluginRuntime rt) {
        return rt.dependenceManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginLoader pluginLoader(PluginRuntime rt) {
        return rt.pluginLoader();
    }
}
```

> 如果项目中已经引入了 `nexus-admin-api`，则无需手写此配置——
> `EventBusAutoConfig`、`ExtensionAutoConfig`、`ConfigAutoConfig`、`PluginAutoConfig`
> 已提供完全等效的桥接。

---

## 覆盖优先级

```
应用层 @Bean  >  api AutoConfig @ConditionalOnMissingBean  >  Runtime.builder()  >  Runtime 默认值
```

---

## 参考文档

- [整体架构](../developer-guid/设计文档/整体架构.md)
- [插件系统](../developer-guid/设计文档/插件系统.md)
- [拓展点系统](../developer-guid/设计文档/拓展点系统.md)
- [事件系统](../developer-guid/设计文档/事件系统.md)
- [配置中心](../developer-guid/设计文档/配置中心.md)
