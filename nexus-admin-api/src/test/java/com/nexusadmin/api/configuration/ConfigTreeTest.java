package com.nexusadmin.api.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTreeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void traversal_shouldPreserveRefSiblingAnnotationsAndInspectCompositions() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "$defs":{"credential":{"type":"object","properties":{
                    "token":{"type":"string"}
                  }}},
                  "allOf":[{"properties":{
                    "credentials":{"$ref":"#/$defs/credential","writeOnly":true}
                  }}]
                }
                """);

        assertEquals(Set.of("/credentials/token"), ConfigTree.schemaPointers(schema));
        assertTrue(ConfigTree.isSensitive(schema, "/credentials"));
        assertTrue(ConfigTree.isSensitive(schema, "/credentials/token"));
        assertTrue(ConfigTree.containsSensitive(schema, ""));
    }

    @Test
    void traversal_shouldStopRecursiveReferencesWithoutOverflow() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {
                  "$schema":"https://json-schema.org/draft/2020-12/schema",
                  "$ref":"#/$defs/node",
                  "$defs":{"node":{"type":"object","properties":{
                    "value":{"type":"string"},
                    "child":{"$ref":"#/$defs/node"}
                  }}}
                }
                """);

        Set<String> pointers = assertDoesNotThrow(() -> ConfigTree.schemaPointers(schema));

        assertEquals(Set.of("/value", "/child"), pointers);
        assertDoesNotThrow(() -> ConfigTree.containsSensitive(schema, ""));
        assertDoesNotThrow(() -> ConfigTree.isSensitive(schema, "/child/value"));
    }
}
