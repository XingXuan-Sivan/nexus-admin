package com.nexusadmin.core.config.store;

import com.nexusadmin.core.exception.ConfigRevisionConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileConfigStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void replaceScope_shouldPreserveTypesAndRejectStaleRevision() {
        FileConfigStore store = new FileConfigStore(tempDir);
        String initialRevision = store.getRevision("demo-plugin");

        String writtenRevision = store.replaceScope("demo-plugin", Map.of(
                "enabled", true,
                "limit", 12,
                "nested", Map.of("names", List.of("a", "b"))
        ), initialRevision);

        assertAll(
                () -> assertNotEquals(initialRevision, writtenRevision),
                () -> assertEquals(true, store.getScope("demo-plugin").get("enabled")),
                () -> assertEquals(12, store.getScope("demo-plugin").get("limit")),
                () -> assertThrows(ConfigRevisionConflictException.class,
                        () -> store.replaceScope("demo-plugin", Map.of("limit", 99), initialRevision)),
                () -> assertEquals(12, store.getScope("demo-plugin").get("limit"))
        );
    }

    @Test
    void replaceScope_shouldCreateBackupAndReadControlledDocument() throws Exception {
        FileConfigStore store = new FileConfigStore(tempDir);
        String first = store.replaceScope("demo-plugin", Map.of("value", 1),
                store.getRevision("demo-plugin"));

        store.replaceDocument("demo-plugin", "json", "{\"value\":2}", first);
        StoredConfigDocument document = store.readDocument("demo-plugin").orElseThrow();

        assertAll(
                () -> assertEquals("json", document.format()),
                () -> assertEquals("{\"value\":2}", document.content()),
                () -> assertTrue(Files.exists(tempDir.resolve("demo-plugin.yml.bak"))),
                () -> assertEquals(2, store.getScope("demo-plugin").get("value"))
        );
    }

    @Test
    void platformDisabledAndDisabledPlugin_shouldUseDistinctFiles() throws Exception {
        FileConfigStore store = new FileConfigStore(tempDir);
        store.replaceScope("platform.disabled", Map.of("disabled-plugins", List.of("demo")),
                store.getRevision("platform.disabled"));
        store.replaceScope("disabled", Map.of("enabled", true),
                store.getRevision("disabled"));

        assertAll(
                () -> assertEquals(List.of("demo"),
                        store.getScope("platform.disabled").get("disabled-plugins")),
                () -> assertEquals(true, store.getScope("disabled").get("enabled")),
                () -> assertTrue(Files.exists(tempDir.resolve("platform.disabled.yml"))),
                () -> assertTrue(Files.exists(tempDir.resolve("disabled.yml"))),
                () -> assertTrue(store.listScopes().containsAll(
                        Set.of("platform.disabled", "disabled")))
        );
    }

    @Test
    void replaceScope_shouldRejectOversizedSerializedDocumentWithoutWriting() {
        FileConfigStore store = new FileConfigStore(tempDir);
        String revision = store.getRevision("demo-plugin");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> store.replaceScope("demo-plugin",
                                Map.of("payload", "x".repeat(512 * 1024)), revision)),
                () -> assertEquals(revision, store.getRevision("demo-plugin")),
                () -> assertFalse(Files.exists(tempDir.resolve("demo-plugin.yml")))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside", "a/b", "a\\b", "..", ".hidden", "trailing.",
            "UPPER", "a..b"})
    void scopeOperations_shouldRejectUnsafeScope(String scope) {
        FileConfigStore store = new FileConfigStore(tempDir);
        assertThrows(IllegalArgumentException.class, () -> store.getRevision(scope));
    }
}
