package com.nexusadmin.core.plugin.discovery.impl;

import com.nexusadmin.core.plugin.discovery.PluginDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonPluginDescriptorParserTest {

    @TempDir
    Path pluginDirectory;

    @Test
    void parse_shouldPreserveImmutableMenuPermissions() throws IOException {
        Path descriptor = pluginDirectory.resolve("src/main/resources/META-INF/plugin.json");
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, """
                {
                  "id": "example-plugin",
                  "version": "1.0.0",
                  "contributes": {
                    "menus": [{
                      "id": "example.settings",
                      "label": "Settings",
                      "route": "/example/settings",
                      "permissions": ["example.view", "example.manage"]
                    }]
                  }
                }
                """);

        PluginDescriptor parsed = new JsonPluginDescriptorParser(List.of(new DevDirectoryFinder()))
                .parse(pluginDirectory);
        List<String> permissions = parsed.contributes().menus().get(0).permissions();

        assertEquals(List.of("example.view", "example.manage"), permissions);
        assertThrows(UnsupportedOperationException.class, () -> permissions.add("example.admin"));
    }

    @Test
    void parse_shouldDefaultMissingMenuPermissionsToImmutableEmptyList() throws IOException {
        Path descriptor = pluginDirectory.resolve("src/main/resources/META-INF/plugin.json");
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, """
                {
                  "id": "legacy-plugin",
                  "version": "1.0.0",
                  "contributes": {
                    "menus": [{
                      "id": "legacy.home",
                      "label": "Home",
                      "route": "/legacy"
                    }]
                  }
                }
                """);

        PluginDescriptor parsed = new JsonPluginDescriptorParser(List.of(new DevDirectoryFinder()))
                .parse(pluginDirectory);
        List<String> permissions = parsed.contributes().menus().get(0).permissions();

        assertEquals(List.of(), permissions);
        assertThrows(UnsupportedOperationException.class, () -> permissions.add("legacy.view"));
    }
}
