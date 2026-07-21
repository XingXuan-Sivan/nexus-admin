package com.nexusadmin.api.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.auth.PermissionAccess;
import com.nexusadmin.core.config.ConfigManager;
import com.nexusadmin.core.config.resolver.ConfigSource;
import com.nexusadmin.core.config.resolver.ResolvedConfigValue;
import com.nexusadmin.core.config.resolver.impl.DefaultConfigSource;
import com.nexusadmin.core.config.resolver.impl.FileConfigSource;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.config.schema.ValidationMessage;
import com.nexusadmin.core.config.schema.impl.JsonSchemaValidator;
import com.nexusadmin.core.config.store.ConfigStore;
import com.nexusadmin.core.config.store.StoredConfigDocument;
import com.nexusadmin.core.exception.ConfigRevisionConflictException;
import com.nexusadmin.core.facade.ConfigFacade;
import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.plugin.loader.PluginWrapper;
import com.nexusadmin.core.runtime.ConfigRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import static com.nexusadmin.api.configuration.ConfigModels.*;

/** 配置中心应用服务，负责契约编排、服务端校验和 scope 级事务。 */
@Service
public final class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final ConfigRuntime runtime;
    private final ConfigManager configManager;
    private final ConfigFacade configFacade;
    private final PluginFacade pluginFacade;
    private final PermissionAccess permissionAccess;
    private final ConfigStore store;
    private final JsonSchemaValidator validator;
    private final ObjectMapper objectMapper;

    public ConfigService(ConfigRuntime runtime,
                         PluginFacade pluginFacade,
                         PermissionAccess permissionAccess,
                         ObjectMapper objectMapper) {
        this.runtime = runtime;
        this.configManager = runtime.configManager();
        this.configFacade = runtime.facade();
        this.pluginFacade = pluginFacade;
        this.permissionAccess = permissionAccess;
        this.store = runtime.configStore();
        this.objectMapper = objectMapper;
        this.validator = new JsonSchemaValidator(objectMapper);
    }

    public Catalog listDomains() {
        Set<String> scopeIds = knownScopeIds();
        List<Domain> domains = scopeIds.stream()
                .map(this::toDomain)
                .sorted(Comparator.comparingInt(this::domainOrder)
                        .thenComparing(Domain::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        String catalogRevision = "catalog-" + Integer.toUnsignedString(domains.hashCode(), 16);
        return new Catalog(catalogRevision, domains);
    }

    public Snapshot getSnapshot(String scopeId) {
        requireDomain(scopeId);
        if (store.supportsDocuments()) {
            Optional<StoredConfigDocument> document = store.readDocument(scopeId);
            if (document.isPresent()) {
                StoredConfigDocument stored = document.get();
                Map<String, Object> persisted = store.parseDocument(
                        stored.format(), stored.content());
                return buildSnapshot(scopeId, persisted, stored.revision());
            }
        }
        Map<String, Object> persisted = configManager.getPersistedScope(scopeId);
        return buildSnapshot(scopeId, persisted, store.getRevision(scopeId));
    }

    public SchemaDocument getSchema(String scopeId) {
        requireDomain(scopeId);
        ConfigSchema schema = configManager.getSchema(scopeId)
                .orElseThrow(() -> new ConfigDomainNotFoundException(scopeId + "/schema"));
        JsonNode document = schema.document();
        return new SchemaDocument(scopeId, ConfigTree.schemaRevision(document),
                schema.dialect(), document);
    }

    public ValidationResult validateChanges(String scopeId,
                                            ChangeRequest request,
                                            String expectedRevision) {
        requireDomain(scopeId);
        assertCurrentRevision(scopeId, expectedRevision);
        PreparedChange prepared = prepareChanges(scopeId, request);
        List<ValidationIssue> issues = validate(scopeId, prepared.persistedValues());
        return new ValidationResult(issues.isEmpty(), issues,
                preview(scopeId, prepared.changedPaths()));
    }

    public Snapshot update(String scopeId,
                           ChangeRequest request,
                           String expectedRevision) {
        requireDomain(scopeId);
        assertCurrentRevision(scopeId, expectedRevision);
        PreparedChange prepared = prepareChanges(scopeId, request);
        List<ValidationIssue> issues = validate(scopeId, prepared.persistedValues());
        if (!issues.isEmpty()) {
            throw new ConfigValidationException(issues);
        }
        String revision = configFacade.replaceScope(scopeId,
                prepared.persistedValues(), expectedRevision,
                prepared.changedPaths());
        log.info("配置域已修改: scope={}, changedPaths={}, reason={}",
                scopeId, prepared.changedPaths(), safeReason(request == null ? null : request.reason()));
        return buildSnapshot(scopeId, prepared.persistedValues(), revision);
    }

    public Snapshot reset(String scopeId,
                          ResetRequest request,
                          String expectedRevision) {
        requireDomain(scopeId);
        if (request == null || (!request.all() && request.paths().isEmpty())) {
            throw new IllegalArgumentException("重置必须指定 paths，整域重置请显式传入 all=true");
        }
        Map<String, Object> persisted = configManager.getPersistedScope(scopeId);
        Set<String> paths = request.all()
                ? ConfigTree.valuePointers(persisted)
                : new LinkedHashSet<>(request.paths());
        JsonNode schema = configManager.getSchema(scopeId)
                .map(ConfigSchema::document)
                .orElseThrow(() -> new IllegalArgumentException("无 Schema 的配置域不能使用结构化重置"));
        List<Change> changes = paths.stream()
                .map(path -> new Change(
                        ConfigTree.isSensitive(schema, path) ? "clear-sensitive" : "unset",
                        path,
                        null))
                .toList();
        return update(scopeId, new ChangeRequest(changes, request.reason()), expectedRevision);
    }

    public Document getDocument(String scopeId) {
        requireDomain(scopeId);
        StoredConfigDocument stored = store.readDocument(scopeId)
                .orElseThrow(() -> new ConfigDomainNotFoundException(scopeId + "/document"));
        boolean sensitive = hasSensitiveFields(scopeId);
        return new Document(
                scopeId,
                stored.displayName(),
                stored.format(),
                stored.revision(),
                sensitive ? "" : stored.content(),
                store.supportsDocuments() && !sensitive,
                sensitive,
                sensitive,
                stored.maxBytes()
        );
    }

    public ValidationResult validateDocument(String scopeId,
                                             DocumentRequest request,
                                             String expectedRevision) {
        requireDocumentEditing(scopeId);
        assertCurrentRevision(scopeId, expectedRevision);
        Map<String, Object> parsed;
        try {
            parsed = store.parseDocument(requireFormat(request), requireContent(scopeId, request));
        } catch (IllegalArgumentException e) {
            ValidationIssue issue = new ValidationIssue(
                    "", "syntax", "配置文档语法无效", null, null);
            return new ValidationResult(false, List.of(issue),
                    new EffectivePreview(List.of(), List.of(), List.of()));
        }
        Set<String> changedPaths = ConfigTree.changedPointers(
                configManager.getPersistedScope(scopeId), parsed);
        List<ValidationIssue> issues = validate(scopeId, parsed);
        return new ValidationResult(issues.isEmpty(), issues, preview(scopeId, changedPaths));
    }

    public DocumentSaveResult saveDocument(String scopeId,
                                           DocumentRequest request,
                                           String expectedRevision) {
        requireDocumentEditing(scopeId);
        assertCurrentRevision(scopeId, expectedRevision);
        Map<String, Object> parsed;
        try {
            parsed = store.parseDocument(requireFormat(request), requireContent(scopeId, request));
        } catch (IllegalArgumentException e) {
            throw new ConfigValidationException(List.of(
                    new ValidationIssue("", "syntax", "配置文档语法无效", null, null)));
        }
        List<ValidationIssue> issues = validate(scopeId, parsed);
        if (!issues.isEmpty()) {
            throw new ConfigValidationException(issues);
        }
        Set<String> changedPaths = ConfigTree.changedPointers(
                configManager.getPersistedScope(scopeId), parsed);
        StoredConfigDocument currentDocument = store.readDocument(scopeId)
                .orElseThrow(() -> new ConfigDomainNotFoundException(scopeId + "/document"));
        String revision = configFacade.replaceDocument(scopeId,
                request.format(), request.content(),
                expectedRevision, changedPaths);
        log.info("配置文档已修改: scope={}, changedPaths={}, reason={}",
                scopeId, changedPaths, safeReason(request.reason()));
        Document document = new Document(
                scopeId,
                currentDocument.displayName(),
                request.format().trim().toLowerCase(Locale.ROOT),
                revision,
                request.content(),
                true,
                false,
                false,
                currentDocument.maxBytes()
        );
        return new DocumentSaveResult(document, buildSnapshot(scopeId, parsed, revision));
    }

    private Domain toDomain(String scopeId) {
        Optional<ConfigSchema> schema = configManager.getSchema(scopeId);
        String schemaStatus = runtime.schemaRegistry().state(scopeId).status();
        PluginWrapper plugin = pluginFacade.getPlugin(scopeId);
        boolean platform = isPlatformScope(scopeId);
        boolean sensitive = schema.map(value -> hasSensitiveFields(value.document())).orElse(false);
        boolean documentSupported = store.supportsDocuments();
        String displayName;
        String description;
        if ("platform".equals(scopeId)) {
            displayName = "平台运行配置";
            description = "配置中心管理的运行期平台策略";
        } else if ("platform.disabled".equals(scopeId)) {
            displayName = "插件禁用状态";
            description = "由插件管理功能维护的禁用插件集合";
        } else if (plugin != null) {
            displayName = plugin.descriptor().name();
            description = plugin.descriptor().description();
        } else {
            displayName = scopeId;
            description = "已持久化的插件配置域";
        }
        Capabilities capabilities = new Capabilities(
                permissionAccess.currentUserHas("config.view"),
                schema.isPresent() && permissionAccess.currentUserHas("config.manage"),
                schema.isPresent() && permissionAccess.currentUserHas("config.manage"),
                schema.isPresent() && permissionAccess.currentUserHas("config.view"),
                documentSupported && permissionAccess.currentUserHas("config.document.view"),
                documentSupported && !sensitive
                        && permissionAccess.currentUserHas("config.document.manage")
        );
        return new Domain(
                scopeId,
                "platform.disabled".equals(scopeId) ? "platform" : null,
                platform ? "platform" : "plugin",
                platform ? null : scopeId,
                displayName,
                description,
                schema.isPresent(),
                schemaStatus,
                documentSupported
                        ? store.readDocument(scopeId).map(StoredConfigDocument::format).orElse(null)
                        : null,
                capabilities,
                true,
                false,
                store.getRevision(scopeId),
                null
        );
    }

    private Snapshot buildSnapshot(String scopeId,
                                   Map<String, Object> persisted,
                                   String revision) {
        Optional<ConfigSchema> schemaOptional = configManager.getSchema(scopeId);
        JsonNode schema = schemaOptional.map(ConfigSchema::document)
                .orElseGet(() -> objectMapper.createObjectNode());
        Map<String, Object> defaults = ConfigTree.schemaDefaults(schema);
        for (ConfigSource source : runtime.configResolver().getSources()) {
            if (source instanceof DefaultConfigSource defaultSource) {
                defaults = ConfigTree.merge(defaults, defaultSource.getConfigMap(scopeId));
            }
        }
        Map<String, Object> effective = ConfigTree.merge(defaults, persisted);
        Set<String> pointers = new LinkedHashSet<>(ConfigTree.schemaPointers(schema));
        pointers.addAll(ConfigTree.valuePointers(defaults));
        pointers.addAll(ConfigTree.valuePointers(persisted));
        Set<String> persistedPointers = ConfigTree.valuePointers(persisted);
        Set<String> defaultPointers = ConfigTree.valuePointers(defaults);

        Map<String, String> resolvedSources = new LinkedHashMap<>();
        Set<String> externallyOverridden = new LinkedHashSet<>();
        for (String pointer : pointers) {
            resolveSnapshotValue(scopeId, pointer, persisted, persistedPointers,
                    defaults, defaultPointers)
                    .ifPresent(resolved -> {
                        Object typed = ConfigTree.coerceExternalValue(
                                resolved.value(), ConfigTree.schemaAt(schema, pointer));
                        ConfigTree.set(effective, pointer, typed);
                        resolvedSources.put(pointer, resolved.source());
                        if (!"file".equals(resolved.source())
                                && !"default".equals(resolved.source())) {
                            externallyOverridden.add(pointer);
                        }
                    });
        }

        Set<String> effectivePointers = ConfigTree.valuePointers(effective);
        pointers.addAll(effectivePointers);

        Map<String, FieldState> states = new LinkedHashMap<>();
        boolean canRotateSecrets = permissionAccess.currentUserHas("config.secret.rotate");
        for (String pointer : pointers) {
            boolean sensitive = ConfigTree.isSensitive(schema, pointer);
            boolean readOnly = ConfigTree.isReadOnly(schema, pointer);
            boolean secretPermissionDenied = sensitive && !canRotateSecrets;
            String source = resolvedSources.getOrDefault(pointer,
                    persistedPointers.contains(pointer) ? "file"
                            : defaultPointers.contains(pointer) ? "default" : "none");
            states.put(pointer, new FieldState(
                    source,
                    effectivePointers.contains(pointer),
                    persistedPointers.contains(pointer),
                    defaultPointers.contains(pointer),
                    !readOnly && !secretPermissionDenied,
                    externallyOverridden.contains(pointer),
                    sensitive,
                    ConfigTree.restartRequired(schema, pointer),
                    readOnly
                            ? "Schema 将该字段声明为只读"
                            : secretPermissionDenied
                                    ? "缺少 config.secret.rotate 权限"
                                    : null
            ));
        }

        return new Snapshot(
                scopeId,
                revision,
                ConfigTree.withoutSensitive(effective, schema, pointers),
                ConfigTree.withoutSensitive(persisted, schema, pointers),
                ConfigTree.withoutSensitive(defaults, schema, pointers),
                Map.copyOf(states)
        );
    }

    private PreparedChange prepareChanges(String scopeId, ChangeRequest request) {
        if (request == null || request.changes().isEmpty()) {
            throw new IllegalArgumentException("changes 不能为空");
        }
        validateReason(request.reason());
        ConfigSchema configSchema = configManager.getSchema(scopeId)
                .orElseThrow(() -> new IllegalArgumentException("无 Schema 的配置域不能使用结构化编辑"));
        JsonNode schema = configSchema.document();
        Map<String, Object> persisted = configManager.getPersistedScope(scopeId);
        Set<String> changedPaths = new LinkedHashSet<>();

        for (Change change : request.changes()) {
            if (change == null || change.op() == null) {
                throw new IllegalArgumentException("每项 change 都必须包含 op");
            }
            String path = change.path();
            JsonNode definition = ConfigTree.schemaAt(schema, path);
            boolean known = !definition.isMissingNode();
            boolean sensitive = known && ConfigTree.isSensitive(schema, path);
            boolean readOnly = known && ConfigTree.isReadOnly(schema, path);
            boolean containsSensitive = known && ConfigTree.containsSensitive(schema, path);
            boolean containsReadOnly = known && ConfigTree.containsReadOnly(schema, path);
            if (readOnly || containsReadOnly) {
                throw new IllegalArgumentException("只读字段不能修改: " + path);
            }
            String operation = change.op().toLowerCase(Locale.ROOT);
            switch (operation) {
                case "set" -> {
                    if (!known) {
                        throw new IllegalArgumentException("Schema 未声明字段: " + path);
                    }
                    if (containsSensitive) {
                        throw new IllegalArgumentException("敏感字段必须使用 replace-sensitive 操作");
                    }
                    ConfigTree.set(persisted, path, change.value());
                }
                case "replace-sensitive" -> {
                    if (!sensitive) {
                        throw new IllegalArgumentException("replace-sensitive 仅适用于敏感字段: " + path);
                    }
                    requireSecretRotationPermission();
                    if (change.value() == null) {
                        throw new IllegalArgumentException("敏感字段替换值不能为空");
                    }
                    ConfigTree.set(persisted, path, change.value());
                }
                case "unset" -> {
                    if (containsSensitive) {
                        throw new IllegalArgumentException("敏感字段必须使用 clear-sensitive 操作");
                    }
                    ConfigTree.remove(persisted, path);
                }
                case "clear-sensitive" -> {
                    if (!sensitive) {
                        throw new IllegalArgumentException("clear-sensitive 仅适用于敏感字段: " + path);
                    }
                    requireSecretRotationPermission();
                    ConfigTree.remove(persisted, path);
                }
                default -> throw new IllegalArgumentException("不支持的配置操作: " + change.op());
            }
            changedPaths.add(path);
        }
        return new PreparedChange(persisted, Set.copyOf(changedPaths));
    }

    private void requireSecretRotationPermission() {
        if (!permissionAccess.currentUserHas("config.secret.rotate")) {
            throw new ConfigPermissionDeniedException("config.secret.rotate");
        }
    }

    private List<ValidationIssue> validate(String scopeId, Map<String, Object> persisted) {
        Optional<ConfigSchema> schema = configManager.getSchema(scopeId);
        if (schema.isEmpty()) {
            return List.of();
        }
        // 文件候选值与外部覆盖后的 effective 都必须合法，防止 ENV 掩盖非法 persisted。
        JsonNode document = schema.get().document();
        Map<String, Object> defaults = defaultValues(scopeId, document);
        Map<String, Object> persistedCandidate = ConfigTree.merge(defaults, persisted);
        Map<String, Object> effective = resolvedValues(
                scopeId, document, defaults, persisted);
        List<ValidationMessage> messages = new ArrayList<>(validator.validate(
                schema.get().document(), objectMapper.valueToTree(persistedCandidate)));
        if (!Objects.deepEquals(persistedCandidate, effective)) {
            validator.validate(schema.get().document(), objectMapper.valueToTree(effective))
                    .stream()
                    .filter(message -> !messages.contains(message))
                    .forEach(messages::add);
        }
        return messages.stream()
                .map(message -> new ValidationIssue(
                        ConfigTree.normalizeValidationPath(message.path()),
                        message.keyword(), message.message(), null, null,
                        null, message.params(), "server", "error"))
                .toList();
    }

    private Map<String, Object> defaultValues(String scopeId, JsonNode schema) {
        Map<String, Object> defaults = ConfigTree.schemaDefaults(schema);
        for (ConfigSource source : runtime.configResolver().getSources()) {
            if (source instanceof DefaultConfigSource defaultSource) {
                defaults = ConfigTree.merge(defaults, defaultSource.getConfigMap(scopeId));
            }
        }
        return defaults;
    }

    private Map<String, Object> resolvedValues(String scopeId,
                                               JsonNode schema,
                                               Map<String, Object> defaults,
                                               Map<String, Object> persisted) {
        Map<String, Object> effective = ConfigTree.merge(defaults, persisted);
        Set<String> pointers = new LinkedHashSet<>(ConfigTree.schemaPointers(schema));
        pointers.addAll(ConfigTree.valuePointers(effective));
        Set<String> persistedPointers = ConfigTree.valuePointers(persisted);
        Set<String> defaultPointers = ConfigTree.valuePointers(defaults);
        for (String pointer : pointers) {
            resolveSnapshotValue(scopeId, pointer, persisted, persistedPointers,
                    defaults, defaultPointers)
                    .ifPresent(value -> ConfigTree.set(effective, pointer,
                            ConfigTree.coerceExternalValue(value.value(),
                                    ConfigTree.schemaAt(schema, pointer))));
        }
        return effective;
    }

    private Optional<ResolvedConfigValue> resolveSnapshotValue(
            String scopeId,
            String pointer,
            Map<String, Object> persisted,
            Set<String> persistedPointers,
            Map<String, Object> defaults,
            Set<String> defaultPointers) {
        String key = ConfigTree.toDotKey(pointer);
        for (ConfigSource source : runtime.configResolver().getSources()) {
            if (source instanceof FileConfigSource) {
                if (persistedPointers.contains(pointer)) {
                    return Optional.of(new ResolvedConfigValue(
                            ConfigTree.get(persisted, pointer), "file"));
                }
                continue;
            }
            if (source instanceof DefaultConfigSource) {
                if (defaultPointers.contains(pointer)) {
                    return Optional.of(new ResolvedConfigValue(
                            ConfigTree.get(defaults, pointer), "default"));
                }
                continue;
            }
            try {
                Optional<Object> value = source.getObject(scopeId, key);
                if (value.isPresent()) {
                    return Optional.of(new ResolvedConfigValue(
                            value.get(), source.sourceType()));
                }
            } catch (RuntimeException e) {
                log.warn("读取配置源失败: source={}, scope={}, path={}",
                        source.name(), scopeId, pointer, e);
            }
        }
        return Optional.empty();
    }

    private EffectivePreview preview(String scopeId, Set<String> changedPaths) {
        Optional<ConfigSchema> schema = configManager.getSchema(scopeId);
        JsonNode document = schema.map(ConfigSchema::document)
                .orElseGet(() -> objectMapper.createObjectNode());
        List<String> overridden = changedPaths.stream()
                .filter(path -> externalOverride(scopeId, path).isPresent())
                .toList();
        List<String> restart = changedPaths.stream()
                .filter(path -> ConfigTree.restartRequired(document, path))
                .toList();
        return new EffectivePreview(List.copyOf(changedPaths), overridden, restart);
    }

    private Optional<ResolvedConfigValue> externalOverride(String scopeId, String pointer) {
        return runtime.configResolver().resolveBeforePriority(
                scopeId, ConfigTree.toDotKey(pointer), 20);
    }

    private void requireDomain(String scopeId) {
        if (scopeId == null || !knownScopeIds().contains(scopeId)) {
            throw new ConfigDomainNotFoundException(scopeId);
        }
    }

    private Set<String> knownScopeIds() {
        Set<String> ids = new LinkedHashSet<>();
        ids.add("platform");
        ids.add("platform.disabled");
        ids.addAll(configManager.getRegisteredSchemaIds());
        ids.addAll(store.listScopes());
        pluginFacade.listPlugins().forEach(plugin -> ids.add(plugin.getPluginId()));
        return ids;
    }

    private boolean hasSensitiveFields(String scopeId) {
        return configManager.getSchema(scopeId)
                .map(ConfigSchema::document)
                .map(this::hasSensitiveFields)
                .orElse(false);
    }

    private boolean hasSensitiveFields(JsonNode schema) {
        return ConfigTree.containsSensitive(schema, "");
    }

    private void requireDocumentEditing(String scopeId) {
        requireDomain(scopeId);
        if (!store.supportsDocuments()) {
            throw new UnsupportedOperationException("当前配置存储不支持原始文档");
        }
        if (hasSensitiveFields(scopeId)) {
            throw new ConfigDocumentLockedException();
        }
    }

    private void assertCurrentRevision(String scopeId, String expectedRevision) {
        String normalized = normalizeRevision(expectedRevision);
        if (normalized == null) {
            throw new IllegalArgumentException("If-Match 请求头不能为空");
        }
        String current = store.getRevision(scopeId);
        if (!current.equals(normalized)) {
            throw new ConfigRevisionConflictException(normalized, current);
        }
    }

    private String normalizeRevision(String revision) {
        if (revision == null || revision.isBlank()) {
            return null;
        }
        String value = revision.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String requireFormat(DocumentRequest request) {
        if (request == null || request.format() == null) {
            throw new IllegalArgumentException("format 不能为空");
        }
        return request.format();
    }

    private String requireContent(String scopeId, DocumentRequest request) {
        if (request == null || request.content() == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        validateReason(request.reason());
        int actualBytes = request.content().getBytes(StandardCharsets.UTF_8).length;
        int maxBytes = store.readDocument(scopeId)
                .map(StoredConfigDocument::maxBytes)
                .orElse(512 * 1024);
        if (actualBytes > maxBytes) {
            throw new ConfigDocumentTooLargeException(actualBytes, maxBytes);
        }
        return request.content();
    }

    private void validateReason(String reason) {
        if (reason != null && reason.length() > 500) {
            throw new IllegalArgumentException("修改原因不能超过 500 个字符");
        }
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "未填写";
        }
        return reason.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private int domainOrder(Domain domain) {
        if ("platform".equals(domain.id())) {
            return 0;
        }
        if (domain.kind().startsWith("platform")) {
            return 1;
        }
        return 2;
    }

    private boolean isPlatformScope(String scopeId) {
        return "platform".equals(scopeId) || "platform.disabled".equals(scopeId);
    }

    private record PreparedChange(Map<String, Object> persistedValues,
                                  Set<String> changedPaths) {
    }
}
