package com.nexusadmin.api.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusadmin.api.exception.GlobalExceptionHandler;
import com.nexusadmin.core.exception.ConfigRevisionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfigProblemContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConfigService configService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ConfigController(configService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void update_shouldReturnStableProblemContract_whenRevisionConflicts() throws Exception {
        doThrow(new ConfigRevisionConflictException("cfg-old", "cfg-current"))
                .when(configService)
                .update(eq("demo-plugin"), any(ConfigModels.ChangeRequest.class), eq("cfg-old"));

        MvcResult result = mockMvc.perform(put("/admin/v1/config/demo-plugin")
                        .header("If-Match", "cfg-old")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {"op": "set", "path": "/limit", "value": 5}
                                  ],
                                  "reason": "contract test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:nexus-admin:problem:config-revision-conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.instance").value("/admin/v1/config/demo-plugin"))
                .andExpect(jsonPath("$.errorCode").value("CONFIG_REVISION_CONFLICT"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.scopeId").value("demo-plugin"))
                .andExpect(jsonPath("$.currentRevision").value("cfg-current"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andReturn();

        JsonNode problem = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertAll(
                () -> assertTrue(problem.path("errorCode").isTextual()),
                () -> assertTrue(problem.path("traceId").isTextual()),
                () -> assertFalse(problem.path("traceId").asText().isBlank())
        );
    }

    @Test
    void validate_shouldReturnSafeFieldErrors_whenConfigurationIsInvalid() throws Exception {
        ConfigModels.ValidationIssue issue = new ConfigModels.ValidationIssue(
                "/limit",
                "maximum",
                "rejected value TOP-SECRET exceeds the limit",
                4,
                7,
                "configuration.validation.maximum",
                Map.of("limit", 10),
                "server",
                "error");
        doThrow(new ConfigValidationException(java.util.List.of(issue)))
                .when(configService)
                .validateChanges(eq("demo-plugin"), any(ConfigModels.ChangeRequest.class), eq("cfg-current"));

        MvcResult result = mockMvc.perform(post("/admin/v1/config/demo-plugin/validate")
                        .header("If-Match", "cfg-current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {"op": "set", "path": "/limit", "value": 99}
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:nexus-admin:problem:config-validation"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.instance").value("/admin/v1/config/demo-plugin/validate"))
                .andExpect(jsonPath("$.errorCode").value("CONFIG_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.scopeId").value("demo-plugin"))
                .andExpect(jsonPath("$.currentRevision").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].path").value("/limit"))
                .andExpect(jsonPath("$.fieldErrors[0].keyword").value("maximum"))
                .andExpect(jsonPath("$.fieldErrors[0].messageKey")
                        .value("configuration.validation.maximum"))
                .andExpect(jsonPath("$.fieldErrors[0].params.limit").value(10))
                .andExpect(jsonPath("$.fieldErrors[0].line").value(4))
                .andExpect(jsonPath("$.fieldErrors[0].column").value(7))
                .andExpect(jsonPath("$.fieldErrors[0].message").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors[0].value").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode problem = objectMapper.readTree(responseBody);
        JsonNode fieldError = problem.path("fieldErrors").get(0);

        assertAll(
                () -> assertTrue(problem.path("errorCode").isTextual()),
                () -> assertTrue(problem.path("traceId").isTextual()),
                () -> assertFalse(problem.path("traceId").asText().isBlank()),
                () -> assertEquals(
                        Set.of("path", "keyword", "messageKey", "params", "line", "column"),
                        objectMapper.convertValue(fieldError, Map.class).keySet()),
                () -> assertFalse(responseBody.contains("TOP-SECRET"))
        );
    }
}
