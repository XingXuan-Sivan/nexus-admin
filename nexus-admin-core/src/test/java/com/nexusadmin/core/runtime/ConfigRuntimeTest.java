package com.nexusadmin.core.runtime;

import com.nexusadmin.core.event.SyncEventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRuntimeTest {

    @TempDir
    Path tempDir;

    @Test
    void build_shouldRegisterPlatformSchemasAtStartup() {
        ConfigRuntime runtime = ConfigRuntime.builder()
                .configDir(tempDir)
                .eventBus(new SyncEventBus())
                .build();

        assertAll(
                () -> assertTrue(runtime.schemaRegistry().contains("platform")),
                () -> assertTrue(runtime.schemaRegistry().contains("platform.disabled")),
                () -> assertEquals("https://json-schema.org/draft/2020-12/schema",
                        runtime.schemaRegistry().get("platform").orElseThrow().dialect()),
                () -> assertTrue(runtime.schemaRegistry().get("platform").orElseThrow()
                        .document().at("/properties/bootstrapPassword/writeOnly").asBoolean()),
                () -> assertFalse(runtime.schemaRegistry().get("platform").orElseThrow()
                        .document().at("/properties/bootstrapPassword").has("default"))
        );
    }
}
