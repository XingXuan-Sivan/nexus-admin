package com.nexusadmin.api.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.auth.PermissionAccess;
import com.nexusadmin.core.config.DefaultConfigManager;
import com.nexusadmin.core.config.resolver.ConfigSource;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.facade.PluginFacade;
import com.nexusadmin.core.runtime.ConfigRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.nexusadmin.api.configuration.ConfigModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConfigRuntime runtime;
    private ConfigService service;
    private PermissionAccess permissionAccess;

    @BeforeEach
    void setUp() throws Exception {
        runtime = ConfigRuntime.builder()
                .configDir(tempDir)
                .eventBus(new SyncEventBus())
                .build();
        PluginFacade pluginFacade = mock(PluginFacade.class);
        permissionAccess = mock(PermissionAccess.class);
        when(permissionAccess.currentUserHas(anyString())).thenReturn(true);
        when(pluginFacade.listPlugins()).thenReturn(List.of());
        registerSchema("test-plugin", """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "additionalProperties":false,
                  "properties":{
                    "enabled":{"type":"boolean","default":true},
                    "limit":{"type":"integer","minimum":1,"maximum":10}
                  }
                }
                """);
        service = new ConfigService(runtime, pluginFacade, permissionAccess, objectMapper);
    }

    @Test
    void update_shouldKeepJsonTypesAndReturnNewRevision() {
        Snapshot initial = service.getSnapshot("test-plugin");

        Snapshot updated = service.update("test-plugin",
                new ChangeRequest(List.of(new Change("set", "/limit", 5)), "测试更新"),
                initial.revision());

        assertAll(
                () -> assertNotEquals(initial.revision(), updated.revision()),
                () -> assertEquals(5, updated.persistedValues().get("limit")),
                () -> assertEquals(true, updated.effectiveValues().get("enabled")),
                () -> assertEquals("file", updated.fieldStates().get("/limit").source())
        );
    }

    @Test
    void validateChanges_shouldRejectValueOutsideSchemaRangeWithoutWriting() {
        Snapshot initial = service.getSnapshot("test-plugin");
        ChangeRequest request = new ChangeRequest(
                List.of(new Change("set", "/limit", 99)), null);

        ValidationResult validation = service.validateChanges(
                "test-plugin", request, initial.revision());

        assertAll(
                () -> assertFalse(validation.valid()),
                () -> assertTrue(validation.issues().stream()
                        .anyMatch(issue -> "maximum".equals(issue.keyword()))),
                () -> assertThrows(ConfigValidationException.class,
                        () -> service.update("test-plugin", request, initial.revision())),
                () -> assertEquals(initial.revision(), service.getSnapshot("test-plugin").revision())
        );
    }

    @Test
    void snapshot_shouldOmitSensitiveValuesAndLockRawDocument() throws Exception {
        registerSchema("secret-plugin", """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{
                    "token":{"type":"string","writeOnly":true,
                      "x-ui-options":{"widget":"password","sensitive":true}}
                  }
                }
                """);
        String revision = runtime.configStore().getRevision("secret-plugin");
        runtime.configStore().replaceScope("secret-plugin", Map.of("token", "never-return"), revision);

        Snapshot snapshot = service.getSnapshot("secret-plugin");
        Document document = service.getDocument("secret-plugin");

        assertAll(
                () -> assertFalse(snapshot.persistedValues().containsKey("token")),
                () -> assertFalse(snapshot.effectiveValues().containsKey("token")),
                () -> assertTrue(snapshot.fieldStates().get("/token").sensitive()),
                () -> assertTrue(snapshot.fieldStates().get("/token").hasPersistedValue()),
                () -> assertTrue(document.redacted()),
                () -> assertFalse(document.writable()),
                () -> assertEquals("", document.content()),
                () -> assertThrows(ConfigDocumentLockedException.class,
                        () -> service.saveDocument("secret-plugin",
                                new DocumentRequest("yaml", "token: changed", null),
                                snapshot.revision()))
        );
    }

    @Test
    void update_shouldRejectStaleRevision() {
        Snapshot initial = service.getSnapshot("test-plugin");
        service.update("test-plugin",
                new ChangeRequest(List.of(new Change("set", "/limit", 4)), null),
                initial.revision());

        assertThrows(com.nexusadmin.core.exception.ConfigRevisionConflictException.class,
                () -> service.update("test-plugin",
                        new ChangeRequest(List.of(new Change("set", "/limit", 6)), null),
                        initial.revision()));
    }

    @Test
    void catalog_shouldTreatPlatformerAsOpaquePluginIdAndReportStoredFormat() throws Exception {
        registerSchema("platformer", """
                {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object",
                 "properties":{"name":{"type":"string"}}}
                """);
        String revision = runtime.configStore().getRevision("platformer");
        service.saveDocument("platformer",
                new DocumentRequest("json", "{\"name\":\"demo\"}", null), revision);

        Domain domain = service.listDomains().domains().stream()
                .filter(item -> "platformer".equals(item.id()))
                .findFirst().orElseThrow();

        assertAll(
                () -> assertEquals("plugin", domain.kind()),
                () -> assertEquals("platformer", domain.pluginId()),
                () -> assertEquals("json", domain.documentFormat()),
                () -> assertEquals("json", service.getDocument("platformer").format())
        );
    }

    @Test
    void documentValidation_shouldUseRequestedScopeSizeLimit() throws Exception {
        registerSchema("platformer", """
                {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}
                """);
        Snapshot snapshot = service.getSnapshot("platformer");
        String oversized = "x".repeat(512 * 1024 + 1);

        assertThrows(ConfigDocumentTooLargeException.class,
                () -> service.validateDocument("platformer",
                        new DocumentRequest("yaml", oversized, null), snapshot.revision()));
    }

    @Test
    void sensitiveFields_shouldRequireDedicatedOperationsAndRemainResettable() throws Exception {
        registerSchema("secret-ops", """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{
                    "token":{"type":"string","writeOnly":true,
                      "x-ui-options":{"sensitive":true}}
                  }
                }
                """);
        Snapshot initial = service.getSnapshot("secret-ops");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.update("secret-ops",
                                new ChangeRequest(List.of(new Change("set", "/token", "secret")), null),
                                initial.revision())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.update("secret-ops",
                                new ChangeRequest(List.of(new Change("unset", "/token", null)), null),
                                initial.revision()))
        );

        Snapshot replaced = service.update("secret-ops",
                new ChangeRequest(List.of(new Change("replace-sensitive", "/token", "secret")), null),
                initial.revision());
        assertEquals("secret", runtime.configStore().getScope("secret-ops").get("token"));

        Snapshot cleared = service.update("secret-ops",
                new ChangeRequest(List.of(new Change("clear-sensitive", "/token", null)), null),
                replaced.revision());
        assertFalse(runtime.configStore().getScope("secret-ops").containsKey("token"));

        Snapshot replacedAgain = service.update("secret-ops",
                new ChangeRequest(List.of(new Change("replace-sensitive", "/token", "secret-again")), null),
                cleared.revision());
        service.reset("secret-ops", new ResetRequest(List.of("/token"), false, null),
                replacedAgain.revision());
        assertFalse(runtime.configStore().getScope("secret-ops").containsKey("token"));
    }

    @Test
    void catalogCapabilities_shouldReflectCurrentUserPermissions() {
        when(permissionAccess.currentUserHas("config.manage")).thenReturn(false);
        when(permissionAccess.currentUserHas("config.document.manage")).thenReturn(false);

        Domain domain = service.listDomains().domains().stream()
                .filter(item -> "test-plugin".equals(item.id()))
                .findFirst().orElseThrow();

        assertAll(
                () -> assertTrue(domain.capabilities().viewValues()),
                () -> assertTrue(domain.capabilities().viewSchema()),
                () -> assertTrue(domain.capabilities().viewDocument()),
                () -> assertFalse(domain.capabilities().editValues()),
                () -> assertFalse(domain.capabilities().resetValues()),
                () -> assertFalse(domain.capabilities().editDocument())
        );
    }

    @Test
    void sensitiveRefSiblingInsideAllOf_shouldBeRedactedAndRejectParentSet() throws Exception {
        registerSchema("composed-secret", """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "$defs":{
                    "credentials":{"type":"object","properties":{
                      "token":{"type":"string"}
                    }}
                  },
                  "allOf":[{"properties":{
                    "credentials":{"$ref":"#/$defs/credentials","writeOnly":true}
                  }}]
                }
                """);
        String revision = runtime.configStore().getRevision("composed-secret");
        runtime.configStore().replaceScope("composed-secret",
                Map.of("credentials", Map.of("token", "never-return")), revision);

        Snapshot snapshot = service.getSnapshot("composed-secret");
        Document document = service.getDocument("composed-secret");

        assertAll(
                () -> assertFalse(snapshot.persistedValues().containsKey("credentials")),
                () -> assertTrue(snapshot.fieldStates().get("/credentials/token").sensitive()),
                () -> assertTrue(document.redacted()),
                () -> assertFalse(document.writable()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.update("composed-secret",
                                new ChangeRequest(List.of(new Change(
                                        "set", "/credentials", Map.of("token", "bypass"))), null),
                                snapshot.revision()))
        );
    }

    @Test
    void sensitiveOperation_shouldRequireRotationPermissionAndExposeReadOnlyFieldState()
            throws Exception {
        registerSchema("permission-secret", """
                {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object",
                 "properties":{"token":{"type":"string","writeOnly":true}}}
                """);
        when(permissionAccess.currentUserHas("config.secret.rotate")).thenReturn(false);

        Snapshot snapshot = service.getSnapshot("permission-secret");
        FieldState state = snapshot.fieldStates().get("/token");

        assertAll(
                () -> assertFalse(state.persistedWritable()),
                () -> assertEquals("缺少 config.secret.rotate 权限", state.readOnlyReason()),
                () -> assertThrows(ConfigPermissionDeniedException.class,
                        () -> service.update("permission-secret",
                                new ChangeRequest(List.of(new Change(
                                        "replace-sensitive", "/token", "secret")), null),
                                snapshot.revision()))
        );
    }

    @Test
    void validation_shouldRejectInvalidPersistedValueEvenWhenExternalSourceMasksIt() {
        ((DefaultConfigManager) runtime.configManager()).registerSource(new ConfigSource() {
            @Override
            public Optional<String> get(String scope, String key) {
                return "test-plugin".equals(scope) && "limit".equals(key)
                        ? Optional.of("5") : Optional.empty();
            }

            @Override
            public int priority() {
                return 5;
            }

            @Override
            public String name() {
                return "test-external";
            }
        });
        Snapshot snapshot = service.getSnapshot("test-plugin");

        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> service.update("test-plugin",
                        new ChangeRequest(List.of(new Change("set", "/limit", 99)), null),
                        snapshot.revision()));

        assertTrue(exception.issues().stream()
                .anyMatch(issue -> "maximum".equals(issue.keyword())));
    }

    @Test
    void snapshot_shouldResolveCustomSourcesAndRuntimeShouldUseSchemaDefaults() {
        ((DefaultConfigManager) runtime.configManager()).registerSource(new ConfigSource() {
            @Override
            public Optional<String> get(String scope, String key) {
                return "test-plugin".equals(scope) && "limit".equals(key)
                        ? Optional.of("7") : Optional.empty();
            }

            @Override
            public int priority() {
                return 50;
            }

            @Override
            public String name() {
                return "remote-config";
            }
        });

        Snapshot snapshot = service.getSnapshot("test-plugin");

        assertAll(
                () -> assertEquals(7L, snapshot.effectiveValues().get("limit")),
                () -> assertEquals("environment", snapshot.fieldStates().get("/limit").source()),
                () -> assertEquals("7", runtime.configManager()
                        .get("test-plugin", "limit").orElseThrow()),
                () -> assertEquals(Boolean.TRUE, runtime.configManager()
                        .get("test-plugin", "enabled", Boolean.class).orElseThrow()),
                () -> assertEquals(Set.of("/enabled", "/limit"),
                        snapshot.fieldStates().keySet())
        );
    }

    @Test
    void sensitiveArrayItems_shouldMakeTheWholeArrayAnAtomicSecret() throws Exception {
        registerSchema("secret-array", """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{
                    "credentials":{
                      "type":"array",
                      "items":{"type":"object","properties":{
                        "label":{"type":"string"},
                        "token":{"type":"string","writeOnly":true}
                      },"required":["label","token"]}
                    }
                  }
                }
                """);
        Snapshot initial = service.getSnapshot("secret-array");
        List<Map<String, String>> credentials = List.of(
                Map.of("label", "primary", "token", "TOP-SECRET-CREDENTIAL"));

        assertThrows(IllegalArgumentException.class,
                () -> service.update("secret-array",
                        new ChangeRequest(List.of(new Change(
                                "set", "/credentials", credentials)), null),
                        initial.revision()));

        Snapshot updated = service.update("secret-array",
                new ChangeRequest(List.of(new Change(
                        "replace-sensitive", "/credentials", credentials)), null),
                initial.revision());

        assertAll(
                () -> assertFalse(updated.effectiveValues().containsKey("credentials")),
                () -> assertFalse(updated.persistedValues().containsKey("credentials")),
                () -> assertTrue(updated.fieldStates().get("/credentials").sensitive()),
                () -> assertTrue(updated.fieldStates().get("/credentials").hasPersistedValue()),
                () -> assertEquals(credentials,
                        runtime.configStore().getScope("secret-array").get("credentials")),
                () -> assertFalse(updated.toString().contains("TOP-SECRET-CREDENTIAL"))
        );

        service.reset("secret-array", new ResetRequest(List.of("/credentials"), false, null),
                updated.revision());
        assertFalse(runtime.configStore().getScope("secret-array").containsKey("credentials"));
    }

    @Test
    void validationResult_shouldNeverInterpolateSensitiveDraftValues() throws Exception {
        registerSchema("secret-validation", """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{
                    "token":{"type":"string","writeOnly":true,"minLength":20}
                  }
                }
                """);
        Snapshot initial = service.getSnapshot("secret-validation");

        ValidationResult result = service.validateChanges("secret-validation",
                new ChangeRequest(List.of(new Change(
                        "replace-sensitive", "/token", "TOP-SECRET")), null),
                initial.revision());

        assertAll(
                () -> assertFalse(result.valid()),
                () -> assertTrue(result.issues().stream()
                        .anyMatch(issue -> "minLength".equals(issue.keyword()))),
                () -> assertFalse(result.toString().contains("TOP-SECRET")),
                () -> assertTrue(result.issues().stream()
                        .allMatch(issue -> issue.messageKey().startsWith(
                                "configuration.validation.")))
        );

        ValidationResult syntaxResult = service.validateDocument("test-plugin",
                new DocumentRequest("yaml", "limit: [TOP-SECRET\n", null),
                service.getSnapshot("test-plugin").revision());
        assertFalse(syntaxResult.valid());
        assertFalse(syntaxResult.toString().contains("TOP-SECRET"));
    }

    private void registerSchema(String scopeId, String json) throws Exception {
        JsonNode document = objectMapper.readTree(json);
        runtime.schemaRegistry().register(scopeId,
                ConfigSchema.fromDocument(scopeId, document, Map.of()));
    }
}
