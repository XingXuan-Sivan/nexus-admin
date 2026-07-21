package com.nexusadmin.core.config.schema.impl;

import com.nexusadmin.core.config.schema.ConfigSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonPluginSchemaProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void load_shouldPreserveNestedSchemaAndUiExtensions() throws Exception {
        Path metaInf = Files.createDirectories(tempDir.resolve("META-INF"));
        Files.writeString(metaInf.resolve("schema.json"), """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "$defs":{"credential":{"type":"object","properties":{"token":{"type":"string","writeOnly":true}}}},
                  "properties":{
                    "connection":{"$ref":"#/$defs/credential","x-ui-group":"security"},
                    "items":{"type":"array","items":{"type":"integer"}}
                  },
                  "if":{"properties":{"enabled":{"const":true}}},
                  "then":{"required":["connection"]}
                }
                """);

        try (URLClassLoader classLoader = new URLClassLoader(
                new java.net.URL[]{tempDir.toUri().toURL()}, null)) {
            ConfigSchema schema = new JsonPluginSchemaProvider()
                    .load("demo-plugin", classLoader).orElseThrow();

            assertAll(
                    () -> assertEquals("security",
                            schema.document().at("/properties/connection/x-ui-group").asText()),
                    () -> assertTrue(schema.document().at("/$defs/credential/properties/token/writeOnly").asBoolean()),
                    () -> assertEquals("integer",
                            schema.document().at("/properties/items/items/type").asText()),
                    () -> assertTrue(schema.document().has("if")),
                    () -> assertTrue(schema.document().has("then"))
            );
        }
    }

    @Test
    void load_shouldRejectRemoteReferences() throws Exception {
        Path metaInf = Files.createDirectories(tempDir.resolve("META-INF"));
        Files.writeString(metaInf.resolve("schema.json"), """
                {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object",
                 "properties":{"remote":{"$ref":"https://example.com/schema.json"}}}
                """);

        try (URLClassLoader classLoader = new URLClassLoader(
                new java.net.URL[]{tempDir.toUri().toURL()}, null)) {
            assertThrows(IllegalArgumentException.class,
                    () -> new JsonPluginSchemaProvider().load("demo-plugin", classLoader));
        }
    }

    @Test
    void load_shouldPreserveTopLevelRefWithoutDirectProperties() throws Exception {
        Path metaInf = Files.createDirectories(tempDir.resolve("META-INF"));
        Files.writeString(metaInf.resolve("schema.json"), """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "$ref":"#/$defs/config",
                  "$defs":{"config":{"type":"object","properties":{"enabled":{"type":"boolean"}}}}
                }
                """);

        try (URLClassLoader classLoader = new URLClassLoader(
                new java.net.URL[]{tempDir.toUri().toURL()}, null)) {
            ConfigSchema schema = new JsonPluginSchemaProvider()
                    .load("demo-plugin", classLoader).orElseThrow();
            assertAll(
                    () -> assertEquals("#/$defs/config", schema.document().path("$ref").asText()),
                    () -> assertEquals("boolean", schema.document()
                            .at("/$defs/config/properties/enabled/type").asText())
            );
        }
    }

    @Test
    void load_shouldRejectSensitiveSchemaDefaults() throws Exception {
        Path metaInf = Files.createDirectories(tempDir.resolve("META-INF"));
        Files.writeString(metaInf.resolve("schema.json"), """
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "type":"object",
                  "properties":{"token":{"type":"string","writeOnly":true,"default":"secret"}}
                }
                """);

        try (URLClassLoader classLoader = new URLClassLoader(
                new java.net.URL[]{tempDir.toUri().toURL()}, null)) {
            assertThrows(IllegalArgumentException.class,
                    () -> new JsonPluginSchemaProvider().load("demo-plugin", classLoader));
        }
    }

    @Test
    void validator_shouldNotIgnoreInvalidDataFieldsStartingWithXDash() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonSchemaValidator validator = new JsonSchemaValidator(mapper);
        var schema = mapper.readTree("""
                {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object",
                 "properties":{"x-timeout":{"type":"integer","maximum":10}}}
                """);
        var data = mapper.readTree("{\"x-timeout\":99}");

        assertTrue(validator.validate(schema, data).stream()
                .anyMatch(issue -> "maximum".equals(issue.keyword())));
    }
}
