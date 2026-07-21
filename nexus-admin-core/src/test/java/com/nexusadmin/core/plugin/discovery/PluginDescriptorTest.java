package com.nexusadmin.core.plugin.discovery;

import com.nexusadmin.core.exception.DescriptorParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginDescriptorTest {

    @ParameterizedTest
    @ValueSource(strings = {"platform", "platform.disabled", "UPPER", "a..b", "trailing."})
    void constructor_shouldRejectReservedOrNonCanonicalPluginIds(String pluginId) {
        assertThrows(DescriptorParseException.class, () -> descriptor(pluginId));
    }

    @Test
    void constructor_shouldAllowOpaqueIdsThatDoNotCollideWithPlatformScopes() {
        assertDoesNotThrow(() -> descriptor("disabled"));
        assertDoesNotThrow(() -> descriptor("platformer"));
        assertDoesNotThrow(() -> descriptor("com.example.demo-plugin"));
    }

    private PluginDescriptor descriptor(String pluginId) {
        return new PluginDescriptor(pluginId, "1.0.0", "test", "", "", "", "",
                Map.of(), Map.of(), Map.of());
    }
}
