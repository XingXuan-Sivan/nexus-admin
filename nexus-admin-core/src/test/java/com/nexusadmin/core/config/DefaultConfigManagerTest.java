package com.nexusadmin.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.core.config.event.ConfigChangedEvent;
import com.nexusadmin.core.config.schema.ConfigSchema;
import com.nexusadmin.core.event.SyncEventBus;
import com.nexusadmin.core.runtime.ConfigRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigManagerTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void composedSensitiveParent_shouldNeverExposeValuesInEvents() throws Exception {
        SyncEventBus eventBus = new SyncEventBus();
        ConfigRuntime runtime = ConfigRuntime.builder()
                .configDir(tempDir)
                .eventBus(eventBus)
                .build();
        runtime.schemaRegistry().register("secret-events", ConfigSchema.fromDocument(
                "secret-events", objectMapper.readTree("""
                        {
                          "$schema":"https://json-schema.org/draft/2020-12/schema",
                          "type":"object",
                          "allOf":[{"properties":{"credentials":{
                            "type":"object","writeOnly":true,
                            "properties":{"token":{"type":"string"}}
                          }}}]
                        }
                        """), Map.of()));
        List<ConfigChangedEvent> events = new CopyOnWriteArrayList<>();
        eventBus.subscribe(ConfigChangedEvent.class, events::add);

        runtime.configManager().replaceScope("secret-events",
                Map.of("credentials", Map.of("token", "TOP-SECRET")), null,
                Set.of("/credentials"));

        ConfigChangedEvent event = events.get(0);
        assertTrue(event.sensitive());
        assertNull(event.value());
        assertNull(event.oldValue());
        assertFalse(event.toString().contains("TOP-SECRET"));
    }

    @Test
    void concurrentWrites_shouldPublishEventsInCommitOrder() throws Exception {
        SyncEventBus eventBus = new SyncEventBus();
        ConfigRuntime runtime = ConfigRuntime.builder()
                .configDir(tempDir)
                .eventBus(eventBus)
                .build();
        CountDownLatch firstEventEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstEvent = new CountDownLatch(1);
        CountDownLatch secondEventObserved = new CountDownLatch(1);
        List<String> values = new CopyOnWriteArrayList<>();
        eventBus.subscribe(ConfigChangedEvent.class, event -> {
            if (!"ordered-scope".equals(event.configScope())) {
                return;
            }
            if ("first".equals(event.value())) {
                firstEventEntered.countDown();
                try {
                    assertTrue(releaseFirstEvent.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            values.add(event.value());
            if ("second".equals(event.value())) {
                secondEventObserved.countDown();
            }
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> runtime.configManager().replaceScope(
                    "ordered-scope", Map.of("value", "first"), null, Set.of("/value")));
            assertTrue(firstEventEntered.await(5, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> runtime.configManager().replaceScope(
                    "ordered-scope", Map.of("value", "second"), null, Set.of("/value")));

            assertFalse(secondEventObserved.await(300, TimeUnit.MILLISECONDS));
            assertFalse(second.isDone());
            releaseFirstEvent.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            assertEquals(List.of("first", "second"), values);
        } finally {
            releaseFirstEvent.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void pluginRegistration_shouldRejectReservedScopesEvenWhenPlatformSchemaIsAbsent() {
        ConfigRuntime runtime = ConfigRuntime.builder()
                .configDir(tempDir)
                .eventBus(new SyncEventBus())
                .build();
        runtime.schemaRegistry().clear();

        assertThrows(IllegalArgumentException.class,
                () -> runtime.configManager().registerPlugin("platform", null));
        assertThrows(IllegalArgumentException.class,
                () -> runtime.configManager().registerPlugin("platform.disabled", null));
    }
}
