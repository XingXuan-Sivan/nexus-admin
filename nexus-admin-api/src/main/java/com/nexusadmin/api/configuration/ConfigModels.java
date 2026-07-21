package com.nexusadmin.api.configuration;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 配置中心 HTTP 契约模型。 */
public final class ConfigModels {

    private ConfigModels() {
    }

    public record Capabilities(
            boolean viewValues,
            boolean editValues,
            boolean resetValues,
            boolean viewSchema,
            boolean viewDocument,
            boolean editDocument
    ) {
    }

    public record Domain(
            String id,
            String parentId,
            String kind,
            String pluginId,
            String displayName,
            String description,
            boolean hasSchema,
            String schemaStatus,
            String documentFormat,
            Capabilities capabilities,
            boolean hotReload,
            boolean restartRequired,
            String revision,
            Instant updatedAt
    ) {
    }

    public record Catalog(String catalogRevision, List<Domain> domains) {
    }

    public record FieldState(
            String source,
            boolean hasEffectiveValue,
            boolean hasPersistedValue,
            boolean hasDefaultValue,
            boolean persistedWritable,
            boolean effectiveValueOverridden,
            boolean sensitive,
            boolean restartRequired,
            String readOnlyReason
    ) {
    }

    public record Snapshot(
            String scopeId,
            String revision,
            Map<String, Object> effectiveValues,
            Map<String, Object> persistedValues,
            Map<String, Object> defaultValues,
            Map<String, FieldState> fieldStates
    ) {
    }

    public record SchemaDocument(
            String scopeId,
            String schemaRevision,
            String dialect,
            JsonNode schema
    ) {
    }

    public record Change(String op, String path, Object value) {
    }

    public record ChangeRequest(List<Change> changes, String reason) {
        public ChangeRequest {
            changes = changes == null ? List.of() : List.copyOf(changes);
        }
    }

    public record ResetRequest(List<String> paths, boolean all, String reason) {
        public ResetRequest {
            paths = paths == null ? List.of() : List.copyOf(paths);
        }
    }

    public record ValidationIssue(
            String path,
            String keyword,
            String message,
            Integer line,
            Integer column,
            String messageKey,
            Map<String, Object> params,
            String source,
            String severity
    ) {
        public ValidationIssue(String path,
                               String keyword,
                               String message,
                               Integer line,
                               Integer column) {
            this(path, keyword, message, line, column, null, Map.of(), "server", "error");
        }

        public ValidationIssue {
            keyword = keyword == null || keyword.isBlank() ? "invalid" : keyword;
            messageKey = messageKey == null || messageKey.isBlank()
                    ? "configuration.validation." + keyword
                    : messageKey;
            params = params == null ? Map.of() : Map.copyOf(params);
            source = source == null ? "server" : source;
            severity = severity == null ? "error" : severity;
        }
    }

    public record EffectivePreview(
            List<String> changedPaths,
            List<String> overriddenPaths,
            List<String> restartRequiredPaths
    ) {
    }

    public record ValidationResult(
            boolean valid,
            List<ValidationIssue> issues,
            EffectivePreview effectivePreview
    ) {
    }

    public record Document(
            String scopeId,
            String displayName,
            String format,
            String revision,
            String content,
            boolean writable,
            boolean redacted,
            boolean requiresReauthentication,
            int maxBytes
    ) {
    }

    public record DocumentRequest(String format, String content, String reason) {
    }

    public record DocumentSaveResult(Document document, Snapshot snapshot) {
    }
}
