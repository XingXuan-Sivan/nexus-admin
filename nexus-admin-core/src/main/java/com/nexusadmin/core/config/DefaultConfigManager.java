package com.nexusadmin.core.config;

import com.nexusadmin.core.config.binder.ConfigUIBuilder;
import com.nexusadmin.core.config.event.ConfigChangedEvent;
import com.nexusadmin.core.config.event.ConfigListener;
import com.nexusadmin.core.config.resolver.ConfigResolver;
import com.nexusadmin.core.config.resolver.ConfigSource;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.SchemaProvider;
import com.nexusadmin.core.config.schema.SchemaRegistry;
import com.nexusadmin.core.config.schema.SchemaValidator;
import com.nexusadmin.core.config.store.ConfigStore;
import com.nexusadmin.core.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusadmin.core.config.resolver.impl.DefaultConfigSource;

/**
 * 默认配置管理器实现，配置中心的统一入口。
 * <p>整合解析器、存储、Schema 注册中心，提供完整的配置管理能力。</p>
 */
public class DefaultConfigManager implements ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigManager.class);

    /**
     * 配置解析器。
     */
    private final ConfigResolver resolver;

    /**
     * Schema 注册中心。
     */
    private final SchemaRegistry schemaRegistry;

    /**
     * 配置存储。
     */
    private final ConfigStore configStore;

    /**
     * 事件总线。
     */
    private final EventBus eventBus;

    /**
     * UI Schema 构建器。
     */
    private final ConfigUIBuilder uiBuilder;

    /**
     * Schema 提供者列表。
     */
    private final List<SchemaProvider> schemaProviders = new CopyOnWriteArrayList<>();

    /**
     * Schema 验证器列表。
     */
    private final List<SchemaValidator> schemaValidators = new CopyOnWriteArrayList<>();

    /**
     * 配置监听器列表。
     */
    private final List<ConfigListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 插件类加载器缓存。
     */
    private final Map<String, ClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();

    /** Serializes one scope mutation through cache invalidation and event publication. */
    private final Map<String, ReentrantLock> mutationLocks = new ConcurrentHashMap<>();

    /**
     * 构造默认配置管理器。
     * <p>使用默认的 UI 构建器，不自动注册默认 Schema 提供者和验证器，
     * 由运行时统一负责注册。</p>
     *
     * @param resolver       配置解析器
     * @param schemaRegistry Schema 注册中心
     * @param configStore    配置存储
     * @param eventBus       事件总线
     */
    public DefaultConfigManager(ConfigResolver resolver,
                                SchemaRegistry schemaRegistry,
                                ConfigStore configStore,
                                EventBus eventBus) {
        this(resolver, schemaRegistry, configStore, eventBus, new ConfigUIBuilder(schemaRegistry));
    }

    /**
     * 构造默认配置管理器，指定 UI 构建器。
     * <p>由 {@link com.nexusadmin.core.runtime.ConfigRuntime} 调用，
     * 不自动注册默认 Schema 提供者和验证器，由运行时统一负责注册。</p>
     *
     * @param resolver       配置解析器
     * @param schemaRegistry Schema 注册中心
     * @param configStore    配置存储
     * @param eventBus       事件总线
     * @param uiBuilder      UI 构建器
     */
    public DefaultConfigManager(ConfigResolver resolver,
                                SchemaRegistry schemaRegistry,
                                ConfigStore configStore,
                                EventBus eventBus,
                                ConfigUIBuilder uiBuilder) {
        this.resolver = Objects.requireNonNull(resolver, "配置解析器不能为空");
        this.schemaRegistry = Objects.requireNonNull(schemaRegistry, "Schema 注册中心不能为空");
        this.configStore = Objects.requireNonNull(configStore, "配置存储不能为空");
        this.eventBus = Objects.requireNonNull(eventBus, "事件总线不能为空");
        this.uiBuilder = Objects.requireNonNull(uiBuilder, "UI 构建器不能为空");

        log.info("配置管理器已初始化");
    }

    @Override
    public Optional<String> get(String scope, String key) {
        return resolver.resolve(scope, key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String scope, String key, Class<T> type) {
        Optional<String> value = get(scope, key);
        if (value.isEmpty()) {
            return Optional.empty();
        }

        String strValue = value.get();
        try {
            Object result = convertValue(strValue, type);
            return Optional.ofNullable((T) result);
        } catch (Exception e) {
            log.warn("配置值转换失败: {}.{} to {}", scope, key, type.getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void set(String scope, String key, String value) {
        ReentrantLock lock = mutationLock(scope);
        lock.lock();
        try {
        Objects.requireNonNull(scope, "作用域不能为空");
        Objects.requireNonNull(key, "键名不能为空");

        // 获取旧值
        String oldValue = get(scope, key).orElse(null);

        // 写入存储
        configStore.set(scope, key, value);

        // 使解析器缓存失效
        resolver.invalidateCache(scope);

        // 发布事件
        boolean sensitive = isSensitive(scope, key);
        ConfigChangedEvent event = new ConfigChangedEvent(scope, key,
                sensitive ? null : value, sensitive ? null : oldValue, sensitive);
        eventBus.publish(event);

        // 通知监听器
        notifyListeners(event);

        log.debug("配置已更新: {}.{}", scope, key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void set(String scope, String key, Object value) {
        if (value == null) {
            remove(scope, key);
            return;
        }
        set(scope, key, value.toString());
    }

    @Override
    public boolean exists(String scope, String key) {
        return get(scope, key).isPresent();
    }

    @Override
    public void remove(String scope, String key) {
        ReentrantLock lock = mutationLock(scope);
        lock.lock();
        try {
        Objects.requireNonNull(scope, "作用域不能为空");
        Objects.requireNonNull(key, "键名不能为空");

        // 获取旧值
        String oldValue = get(scope, key).orElse(null);

        // 从存储中删除
        configStore.remove(scope, key);

        // 使解析器缓存失效
        resolver.invalidateCache(scope);

        // 发布事件
        boolean sensitive = isSensitive(scope, key);
        ConfigChangedEvent event = new ConfigChangedEvent(scope, key, null,
                sensitive ? null : oldValue, sensitive);
        eventBus.publish(event);

        // 通知监听器
        notifyListeners(event);

        log.debug("配置已删除: {}.{}", scope, key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String replaceScope(String scope,
                               Map<String, Object> values,
                               String expectedRevision,
                               Set<String> changedPaths) {
        ReentrantLock lock = mutationLock(scope);
        lock.lock();
        try {
        Objects.requireNonNull(scope, "作用域不能为空");
        Objects.requireNonNull(values, "配置值不能为空");
        Set<String> paths = changedPaths == null ? Set.of() : Set.copyOf(changedPaths);
        Map<String, Object> previous = configStore.getScope(scope);
        String revision = configStore.replaceScope(scope, values, expectedRevision);
        publishScopeChanges(scope, previous, values, paths);
        log.debug("配置域已更新: scope={}, changedPaths={}, revision={}",
                scope, paths.size(), revision);
        return revision;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String replaceDocument(String scope,
                                  String format,
                                  String content,
                                  String expectedRevision,
                                  Set<String> changedPaths) {
        ReentrantLock lock = mutationLock(scope);
        lock.lock();
        try {
        Objects.requireNonNull(scope, "作用域不能为空");
        Map<String, Object> previous = configStore.getScope(scope);
        String revision = configStore.replaceDocument(scope, format, content, expectedRevision);
        Map<String, Object> current = configStore.getScope(scope);
        Set<String> paths = changedPaths == null ? Set.of() : Set.copyOf(changedPaths);
        publishScopeChanges(scope, previous, current, paths);
        log.debug("配置文档已更新: scope={}, changedPaths={}, revision={}",
                scope, paths.size(), revision);
        return revision;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, Object> getPersistedScope(String scope) {
        return configStore.getScope(scope);
    }

    @Override
    public String getRevision(String scope) {
        return configStore.getRevision(scope);
    }

    @Override
    public Optional<ConfigSchema> getSchema(String schemaId) {
        return schemaRegistry.get(schemaId);
    }

    @Override
    public Map<String, Object> buildUISchema(String schemaId) {
        return uiBuilder.build(schemaId);
    }

    @Override
    public void registerPlugin(String pluginId, ClassLoader classLoader) {
        Objects.requireNonNull(pluginId, "插件ID不能为空");
        ConfigScopeIds.requireValid(pluginId);
        if (ConfigScopeIds.isReserved(pluginId)) {
            throw new IllegalArgumentException("平台保留配置域不能由插件覆盖: " + pluginId);
        }

        // 缓存类加载器
        if (classLoader != null) {
            pluginClassLoaders.put(pluginId, classLoader);
            resolver.getSources().stream()
                    .filter(DefaultConfigSource.class::isInstance)
                    .map(DefaultConfigSource.class::cast)
                    .forEach(source -> source.registerClassLoader(pluginId, classLoader));
        }

        // 加载并注册 Schema
        boolean schemaLoaded = false;
        boolean schemaInvalid = false;
        for (SchemaProvider provider : schemaProviders) {
            try {
                Optional<ConfigSchema> schema = provider.load(pluginId, classLoader);
                if (schema.isPresent()) {
                    schemaRegistry.register(pluginId, schema.get());
                    schemaLoaded = true;
                    break; // 使用第一个成功加载的提供者
                }
            } catch (Exception e) {
                log.warn("Schema 提供者 {} 加载失败: {}", provider.name(), pluginId, e);
                schemaRegistry.markInvalid(pluginId, "Schema 加载或校验失败");
                schemaInvalid = true;
                break;
            }
        }
        if (!schemaLoaded && !schemaInvalid) {
            schemaRegistry.markMissing(pluginId);
        }

        log.debug("插件配置已注册: {}", pluginId);
    }

    @Override
    public void unregisterPlugin(String pluginId) {
        if (ConfigScopeIds.isReserved(pluginId)) {
            throw new IllegalArgumentException("平台保留配置域不能注销: " + pluginId);
        }
        schemaRegistry.unregister(pluginId);
        pluginClassLoaders.remove(pluginId);
        resolver.getSources().stream()
                .filter(DefaultConfigSource.class::isInstance)
                .map(DefaultConfigSource.class::cast)
                .forEach(source -> source.unregisterClassLoader(pluginId));
        log.debug("插件配置已注销: {}", pluginId);
    }

    /**
     * 禁用插件配置作用域。
     */
    private static final String DISABLED_SCOPE = "platform.disabled";

    /**
     * 禁用插件列表键名。
     */
    private static final String DISABLED_PLUGINS_KEY = "disabled-plugins";

    @Override
    public boolean isPluginDisabled(String pluginId) {
        java.util.List<String> disabledList = getDisabledPluginsList();
        return disabledList.contains(pluginId);
    }

    @Override
    public void setPluginDisabled(String pluginId, boolean disabled) {
        ReentrantLock lock = mutationLock(DISABLED_SCOPE);
        lock.lock();
        try {
        java.util.Set<String> disabledSet = new java.util.LinkedHashSet<>(getDisabledPluginsList());

        if (disabled) {
            disabledSet.add(pluginId);
        } else {
            disabledSet.remove(pluginId);
        }

        // 存储为 YAML 列表格式
        configStore.set(DISABLED_SCOPE, DISABLED_PLUGINS_KEY, new java.util.ArrayList<>(disabledSet));
        resolver.invalidateCache(DISABLED_SCOPE);

        log.debug("插件禁用状态已更新: {} = {}", pluginId, disabled);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取禁用插件列表。
     *
     * @return 禁用插件ID列表
     */
    @SuppressWarnings("unchecked")
    private java.util.List<String> getDisabledPluginsList() {
        Map<String, Object> config = configStore.getScope(DISABLED_SCOPE);
        Object value = config.get(DISABLED_PLUGINS_KEY);

        if (value instanceof java.util.List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }

        return java.util.Collections.emptyList();
    }

    @Override
    public void addListener(ConfigListener listener) {
        Objects.requireNonNull(listener, "监听器不能为空");
        listeners.add(listener);
        log.debug("已添加配置监听器: {}", listener.getClass().getSimpleName());
    }

    @Override
    public void removeListener(ConfigListener listener) {
        listeners.remove(listener);
        log.debug("已移除配置监听器: {}", listener.getClass().getSimpleName());
    }

    /**
     * 注册配置源。
     *
     * @param source 配置源
     */
    public void registerSource(ConfigSource source) {
        resolver.addSource(source);
    }

    /**
     * 注册 Schema 提供者。
     *
     * @param provider Schema 提供者
     */
    public void registerSchemaProvider(SchemaProvider provider) {
        schemaProviders.add(provider);
        schemaProviders.sort(java.util.Comparator.comparingInt(SchemaProvider::priority));
        log.debug("已注册 Schema 提供者: {}", provider.name());
    }

    /**
     * 注册 Schema 验证器。
     *
     * @param validator Schema 验证器
     */
    public void registerSchemaValidator(SchemaValidator validator) {
        schemaValidators.add(validator);
        schemaValidators.sort(java.util.Comparator.comparingInt(SchemaValidator::priority));
        log.debug("已注册 Schema 验证器: {}", validator.name());
    }

    /**
     * 获取插件类加载器。
     *
     * @param pluginId 插件ID
     * @return 类加载器，可能为 null
     */
    public ClassLoader getPluginClassLoader(String pluginId) {
        return pluginClassLoaders.get(pluginId);
    }

    /**
     * 通知所有监听器。
     *
     * @param event 配置变更事件
     */
    private void notifyListeners(ConfigChangedEvent event) {
        for (ConfigListener listener : listeners) {
            try {
                String interestedScope = listener.interestedScope();
                if (interestedScope == null || interestedScope.isEmpty()
                        || event.configScope().startsWith(interestedScope)) {
                    listener.onConfigChanged(event);
                }
            } catch (Exception e) {
                log.warn("配置监听器处理失败: {}", listener.getClass().getName(), e);
            }
        }
    }

    /**
     * 转换配置值到目标类型。
     *
     * @param value 字符串值
     * @param type  目标类型
     * @return 转换后的值
     */
    private Object convertValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        }
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        }
        if (type == Long.class || type == long.class) {
            return Long.parseLong(value);
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        }
        if (type == Float.class || type == float.class) {
            return Float.parseFloat(value);
        }
        if (type.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Enum enumValue = Enum.valueOf((Class<Enum>) type, value);
            return enumValue;
        }
        throw new IllegalArgumentException("不支持的类型转换: " + type.getName());
    }

    /**
     * 获取配置解析器。
     *
     * @return 配置解析器
     */
    public ConfigResolver getResolver() {
        return resolver;
    }

    @Override
    public Set<String> getRegisteredSchemaIds() {
        return schemaRegistry.getRegisteredSchemaIds();
    }

    /**
     * 获取 Schema 注册中心。
     *
     * @return Schema 注册中心
     */
    public SchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }

    /**
     * 获取配置存储。
     *
     * @return 配置存储
     */
    public ConfigStore getConfigStore() {
        return configStore;
    }

    private boolean isSensitive(String scope, String key) {
        Optional<ConfigSchema> schema = schemaRegistry.get(scope);
        if (schema.isEmpty()) {
            return false;
        }
        JsonNode root = schema.get().document();
        JsonNode node = dereference(root, root);
        if (sensitiveNode(root) || sensitiveNode(node)) {
            return true;
        }
        for (String segment : key.split("\\.")) {
            if (segment.isEmpty()) {
                continue;
            }
            JsonNode rawNode = node.path("properties").path(segment);
            if (rawNode.isMissingNode()) {
                // 对组合 Schema 中无法按 properties 直接定位的路径采取保守脱敏。
                return containsSensitive(root, root, new java.util.HashSet<>(), 0);
            }
            node = dereference(root, rawNode);
            if (sensitiveNode(rawNode) || sensitiveNode(node)) {
                return true;
            }
        }
        return containsSensitive(root, node, new java.util.HashSet<>(), 0);
    }

    private boolean containsSensitive(JsonNode root,
                                      JsonNode node,
                                      Set<String> visitedReferences,
                                      int depth) {
        if (node == null || node.isMissingNode() || depth > 64) {
            return false;
        }
        if (sensitiveNode(node)) {
            return true;
        }
        JsonNode reference = node.path("$ref");
        if (reference.isTextual() && reference.asText().startsWith("#")
                && visitedReferences.add(reference.asText())) {
            JsonNode resolved = root.at(reference.asText().substring(1));
            if (containsSensitive(root, resolved, visitedReferences, depth + 1)) {
                return true;
            }
        }
        if (node.isContainerNode()) {
            var children = node.elements();
            while (children.hasNext()) {
                if (containsSensitive(root, children.next(), visitedReferences, depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonNode dereference(JsonNode root, JsonNode node) {
        JsonNode reference = node.path("$ref");
        if (!reference.isTextual() || !reference.asText().startsWith("#")) {
            return node;
        }
        JsonNode resolved = root.at(reference.asText().substring(1));
        return resolved.isMissingNode() ? node : resolved;
    }

    private boolean sensitiveNode(JsonNode node) {
        return node.path("writeOnly").asBoolean(false)
                || node.path("x-ui-options").path("sensitive").asBoolean(false);
    }

    private String pointerToKey(String pointer) {
        if (pointer == null || pointer.isBlank() || "/".equals(pointer)) {
            return "";
        }
        String normalized = pointer.startsWith("/") ? pointer.substring(1) : pointer;
        return java.util.Arrays.stream(normalized.split("/"))
                .map(segment -> segment.replace("~1", "/").replace("~0", "~"))
                .reduce((left, right) -> left + "." + right)
                .orElse("");
    }

    private Object nestedValue(Map<String, Object> source, String key) {
        Object current = source;
        for (String segment : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private void publishScopeChanges(String scope,
                                     Map<String, Object> previous,
                                     Map<String, Object> current,
                                     Set<String> paths) {
        resolver.invalidateCache(scope);
        for (String path : paths) {
            String key = pointerToKey(path);
            boolean sensitive = isSensitive(scope, key);
            String oldValue = sensitive ? null : valueAsString(nestedValue(previous, key));
            String newValue = sensitive ? null : valueAsString(nestedValue(current, key));
            ConfigChangedEvent event = new ConfigChangedEvent(
                    scope, key, newValue, oldValue, sensitive);
            eventBus.publish(event);
            notifyListeners(event);
        }
    }

    private ReentrantLock mutationLock(String scope) {
        ConfigScopeIds.requireValid(scope);
        return mutationLocks.computeIfAbsent(scope, ignored -> new ReentrantLock(true));
    }
}

