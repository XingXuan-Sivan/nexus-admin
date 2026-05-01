# 独立使用 Core 模块

---

## 概述

`nexus-admin-core` 是 Nexus Admin 的微内核运行时模块，可脱离 Spring 框架独立使用。
通过 `CoreRuntime` 聚合根获得全部核心组件的默认装配，也可通过 Builder 选择性覆盖。

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

```java
// 一行代码获得全部核心组件
CoreRuntime rt = CoreRuntime.defaults();

EventBus eventBus = rt.eventBus();
ExtensionRegistry extensionRegistry = rt.extensionRegistry();
PluginRegistry pluginRegistry = rt.pluginRegistry();
VersionManager versionManager = rt.versionManager();
DependenceManager dependenceManager = rt.dependenceManager();
PluginLoader pluginLoader = rt.pluginLoader();
```

### 选择性覆盖

```java
CoreRuntime rt = CoreRuntime.builder()
        .eventBus(new MyAsyncEventBus())            // 替换默认的同步事件总线
        .versionManager(new SemanticVersionManager()) // 替换版本管理器
        .build();
```

未指定的组件使用默认实现。依赖关系自动注入（如 `DependenceManager` 自动获得覆盖后的 `VersionManager`）。

### 与 PluginManager 集成

```java
CoreRuntime rt = CoreRuntime.defaults();

PluginManager pluginManager = new DefaultPluginManager(
        rt.pluginRegistry(),
        rt.extensionRegistry(),
        rt.eventBus(),
        RuntimeMode.DEV,
        Paths.get("plugins-data"),
        sourcesList,
        findersList,
        parsersList,
        rt.versionManager(),
        rt.dependenceManager(),
        rt.pluginLoader()
);
```

---

## Spring 环境使用

### 全默认（开箱即用）

引入 `nexus-admin-api` 模块后，`CoreAutoConfig` 自动将核心组件桥接到 Spring 容器。
无需额外配置即可通过 `@Autowired` 注入：

```java
@Autowired
private EventBus eventBus;

@Autowired
private ExtensionRegistry extensionRegistry;
```

### 覆盖单个组件

声明同类型 Bean 即可覆盖默认值：

```java
@Configuration
public class MyConfig {

    @Bean
    public EventBus eventBus() {
        // Spring 容器将使用此 Bean 而非 CoreRuntime 中的默认值
        return new AsyncEventBus();
    }
}
```

### 一次性替换全部组件

声明 `CoreRuntime` Bean 可级联替换所有子组件：

```java
@Configuration
public class MyConfig {

    @Bean
    public CoreRuntime coreRuntime() {
        return CoreRuntime.builder()
                .eventBus(new AsyncEventBus())
                .pluginLoader(new CustomPluginLoader())
                .build();
    }
}
```

### 不引入 api 模块的手动桥接

若仅依赖 `nexus-admin-core` 而未引入 `nexus-admin-api`，
需要自行编写 Spring 配置类将 CoreRuntime 桥接到容器：

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
import com.nexusadmin.core.CoreRuntime;
import com.nexusadmin.core.event.EventBus;
import com.nexusadmin.core.extension.ExtensionRegistry;
import com.nexusadmin.core.plugin.PluginRegistry;
import com.nexusadmin.core.plugin.loader.PluginLoader;
import com.nexusadmin.core.plugin.resolve.DependenceManager;
import com.nexusadmin.core.plugin.resolve.VersionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 CoreRuntime 桥接到 Spring 容器的手动配置。
 * <p>职责与 api 模块中的 CoreAutoConfig 相同，由使用者自行维护。</p>
 */
@Configuration
public class CoreRuntimeBridgeConfig {

    @Bean
    @ConditionalOnMissingBean
    public CoreRuntime coreRuntime() {
        return CoreRuntime.defaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventBus eventBus(CoreRuntime rt) {
        return rt.eventBus();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry(CoreRuntime rt) {
        return rt.extensionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry(CoreRuntime rt) {
        return rt.pluginRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public VersionManager versionManager(CoreRuntime rt) {
        return rt.versionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public DependenceManager dependenceManager(CoreRuntime rt) {
        return rt.dependenceManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginLoader pluginLoader(CoreRuntime rt) {
        return rt.pluginLoader();
    }
}
```

> 如果项目中已经引入了 `nexus-admin-api`，则无需手写此配置——
> `CoreAutoConfig` 已提供完全等效的桥接。

---

## 覆盖优先级

```
应用层 @Bean  >  api 层 @ConditionalOnMissingBean  >  CoreRuntime 默认值
```

---

## 参考文档

- [整体架构](../developer-guid/设计文档/整体架构.md)
- [插件系统](../developer-guid/设计文档/插件系统.md)
- [拓展点系统](../developer-guid/设计文档/拓展点系统.md)
- [事件系统](../developer-guid/设计文档/事件系统.md)
- [配置中心](../developer-guid/设计文档/配置中心.md)
