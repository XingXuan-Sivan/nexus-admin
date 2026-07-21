package com.nexusadmin.api.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 配置树与 JSON Pointer 的无状态操作。 */
final class ConfigTree {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_SCHEMA_DEPTH = 64;
    private static final List<String> COMPOSITION_KEYWORDS = List.of(
            "allOf", "anyOf", "oneOf"
    );
    private static final List<String> CONDITIONAL_KEYWORDS = List.of(
            "if", "then", "else", "dependentSchemas"
    );

    private ConfigTree() {
    }

    static Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, copyValue(value)));
        return result;
    }

    static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> overlay) {
        Map<String, Object> result = deepCopy(base);
        overlay.forEach((key, value) -> {
            Object current = result.get(key);
            if (current instanceof Map<?, ?> currentMap && value instanceof Map<?, ?> overlayMap) {
                result.put(key, merge(stringMap(currentMap), stringMap(overlayMap)));
            } else {
                result.put(key, copyValue(value));
            }
        });
        return result;
    }

    static Map<String, Object> schemaDefaults(JsonNode schema) {
        Object value = defaultValue(schema, schema);
        return value instanceof Map<?, ?> map ? stringMap(map) : new LinkedHashMap<>();
    }

    static Set<String> schemaPointers(JsonNode schema) {
        Set<String> result = new LinkedHashSet<>();
        collectSchemaPointers(schema, schema, "", result,
                new LinkedHashSet<>(), 0);
        return result;
    }

    static Set<String> valuePointers(Map<String, Object> values) {
        Set<String> result = new LinkedHashSet<>();
        collectValuePointers(values, "", result);
        return result;
    }

    static Set<String> changedPointers(Map<String, Object> before, Map<String, Object> after) {
        Set<String> pointers = new LinkedHashSet<>(valuePointers(before));
        pointers.addAll(valuePointers(after));
        pointers.removeIf(pointer -> Objects.deepEquals(get(before, pointer), get(after, pointer)));
        return pointers;
    }

    static JsonNode schemaAt(JsonNode root, String pointer) {
        List<JsonNode> candidates = nodesAt(root, pointer);
        if (candidates.isEmpty()) {
            return MissingNode.getInstance();
        }
        List<JsonNode> applicable = new ArrayList<>();
        for (JsonNode candidate : candidates) {
            collectApplicableNodes(root, candidate, applicable,
                    new LinkedHashSet<>(), 0);
        }
        return applicable.stream()
                .filter(node -> node.has("type") || node.has("properties")
                        || node.has("default") || node.has("enum") || node.has("const"))
                .findFirst()
                .orElse(candidates.get(0));
    }

    static boolean isSensitive(JsonNode schema, String pointer) {
        if (hasAnnotationOnPath(schema, pointer, true)) {
            return true;
        }
        // Arrays containing sensitive items are managed as one atomic secret. This avoids
        // leaking item values through positional JSON Pointers when items are added/reordered.
        for (JsonNode candidate : nodesAt(schema, pointer)) {
            List<JsonNode> applicable = new ArrayList<>();
            collectApplicableNodes(schema, candidate, applicable,
                    new LinkedHashSet<>(), 0);
            boolean sensitiveArray = applicable.stream()
                    .anyMatch(node -> isArraySchema(node)
                            && containsAnnotation(schema, node, true,
                            new LinkedHashSet<>(), 0));
            if (sensitiveArray) {
                return true;
            }
        }
        return false;
    }

    static boolean isReadOnly(JsonNode schema, String pointer) {
        return hasAnnotationOnPath(schema, pointer, false);
    }

    static boolean containsSensitive(JsonNode schema, String pointer) {
        return nodesAt(schema, pointer).stream()
                .anyMatch(node -> containsAnnotation(schema, node, true,
                        new LinkedHashSet<>(), 0));
    }

    static boolean containsReadOnly(JsonNode schema, String pointer) {
        return nodesAt(schema, pointer).stream()
                .anyMatch(node -> containsAnnotation(schema, node, false,
                        new LinkedHashSet<>(), 0));
    }

    static boolean restartRequired(JsonNode schema, String pointer) {
        return nodesAt(schema, pointer).stream()
                .anyMatch(node -> hasRestartAnnotation(schema, node,
                        new LinkedHashSet<>(), 0));
    }

    static Object coerceExternalValue(Object value, JsonNode definition) {
        if (!(value instanceof String text) || definition == null || definition.isMissingNode()) {
            return value;
        }
        String type = definition.path("type").asText("string");
        try {
            return switch (type) {
                case "integer" -> Long.parseLong(text);
                case "number" -> Double.parseDouble(text);
                case "boolean" -> {
                    if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                        yield value;
                    }
                    yield Boolean.parseBoolean(text);
                }
                case "array", "object" -> MAPPER.readValue(text, Object.class);
                case "null" -> "null".equalsIgnoreCase(text) ? null : value;
                default -> value;
            };
        } catch (Exception ignored) {
            return value;
        }
    }

    static Object get(Map<String, Object> root, String pointer) {
        Object current = root;
        for (String segment : segments(pointer)) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    static void set(Map<String, Object> root, String pointer, Object value) {
        List<String> segments = segments(pointer);
        requireFieldPointer(pointer, segments);
        Map<String, Object> current = root;
        for (int index = 0; index < segments.size() - 1; index++) {
            String segment = segments.get(index);
            Object child = current.get(segment);
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                current.put(segment, child);
            }
            current = (Map<String, Object>) child;
        }
        current.put(segments.get(segments.size() - 1), copyValue(value));
    }

    @SuppressWarnings("unchecked")
    static void remove(Map<String, Object> root, String pointer) {
        List<String> segments = segments(pointer);
        requireFieldPointer(pointer, segments);
        Map<String, Object> current = root;
        for (int index = 0; index < segments.size() - 1; index++) {
            Object child = current.get(segments.get(index));
            if (!(child instanceof Map<?, ?>)) {
                return;
            }
            current = (Map<String, Object>) child;
        }
        current.remove(segments.get(segments.size() - 1));
    }

    static String toDotKey(String pointer) {
        return String.join(".", segments(pointer));
    }

    static String normalizeValidationPath(String path) {
        if (path == null || path.isBlank() || "$".equals(path)) {
            return "";
        }
        if (path.startsWith("/")) {
            return path;
        }
        String normalized = path.startsWith("$.") ? path.substring(2) : path;
        if (normalized.startsWith("$[")) {
            normalized = normalized.substring(1);
        }
        return "/" + normalized.replace(".", "/")
                .replaceAll("\\[([0-9]+)]", "/$1");
    }

    static String schemaRevision(JsonNode schema) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(schema.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder("schema-");
            for (int index = 0; index < 12; index++) {
                value.append(String.format("%02x", digest[index]));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("运行环境缺少 SHA-256", e);
        }
    }

    static Map<String, Object> withoutSensitive(Map<String, Object> source,
                                                JsonNode schema,
                                                Collection<String> pointers) {
        Map<String, Object> result = deepCopy(source);
        Set<String> candidates = new LinkedHashSet<>(pointers);
        candidates.addAll(valuePointers(source));
        Set<String> pathsAndAncestors = new LinkedHashSet<>();
        candidates.forEach(pointer -> pathsAndAncestors.addAll(pointerPrefixes(pointer)));
        pathsAndAncestors.stream()
                .filter(pointer -> isSensitive(schema, pointer))
                .forEach(pointer -> remove(result, pointer));
        return result;
    }

    private static Object defaultValue(JsonNode root, JsonNode definition) {
        return defaultValue(root, definition, new LinkedHashSet<>(), 0);
    }

    private static Object defaultValue(JsonNode root,
                                       JsonNode node,
                                       Set<String> referenceStack,
                                       int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            return null;
        }
        if (node.has("default")) {
            return MAPPER.convertValue(node.get("default"), Object.class);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        mergePropertyDefaults(root, node.path("properties"), result,
                referenceStack, depth);

        String reference = localReference(node);
        if (reference != null && referenceStack.add(reference)) {
            Object referenced = defaultValue(root, resolveReference(root, reference),
                    referenceStack, depth + 1);
            if (referenced instanceof Map<?, ?> map) {
                result = merge(stringMap(map), result);
            }
            referenceStack.remove(reference);
        }
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                for (JsonNode schema : schemas) {
                    Object nested = defaultValue(root, schema, referenceStack, depth + 1);
                    if (nested instanceof Map<?, ?> map) {
                        result = merge(result, stringMap(map));
                    }
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static void mergePropertyDefaults(JsonNode root,
                                              JsonNode properties,
                                              Map<String, Object> result,
                                              Set<String> referenceStack,
                                              int depth) {
        if (!properties.isObject()) {
            return;
        }
        properties.fields().forEachRemaining(entry -> {
            Object child = defaultValue(root, entry.getValue(), referenceStack, depth + 1);
            if (child != null) {
                result.put(entry.getKey(), child);
            }
        });
    }

    private static boolean collectSchemaPointers(JsonNode root,
                                                 JsonNode node,
                                                 String parent,
                                                 Set<String> result,
                                                 Set<String> referenceStack,
                                                 int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            if (!parent.isEmpty()) {
                result.add(parent);
                return true;
            }
            return false;
        }
        boolean contributed = false;
        JsonNode properties = node.path("properties");
        if (properties.isObject()) {
            var fields = properties.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                collectSchemaPointers(root, entry.getValue(),
                        parent + "/" + escape(entry.getKey()), result,
                        referenceStack, depth + 1);
                contributed = true;
            }
        }

        String reference = localReference(node);
        if (reference != null && referenceStack.add(reference)) {
            contributed |= collectSchemaPointers(root, resolveReference(root, reference),
                    parent, result, referenceStack, depth + 1);
            referenceStack.remove(reference);
        }
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                for (JsonNode schema : schemas) {
                    contributed |= collectSchemaPointers(root, schema, parent, result,
                            referenceStack, depth + 1);
                }
            }
        }
        for (String keyword : CONDITIONAL_KEYWORDS) {
            JsonNode nested = node.path(keyword);
            if (nested.isObject()) {
                if ("dependentSchemas".equals(keyword)) {
                    for (JsonNode schema : nested) {
                        contributed |= collectSchemaPointers(root, schema, parent, result,
                                referenceStack, depth + 1);
                    }
                } else {
                    contributed |= collectSchemaPointers(root, nested, parent, result,
                            referenceStack, depth + 1);
                }
            }
        }
        if (!contributed && !parent.isEmpty()) {
            result.add(parent);
            return true;
        }
        return contributed;
    }

    private static void collectValuePointers(Object value,
                                             String parent,
                                             Set<String> result) {
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            map.forEach((key, child) -> collectValuePointers(
                    child, parent + "/" + escape(String.valueOf(key)), result));
            return;
        }
        if (!parent.isEmpty()) {
            result.add(parent);
        }
    }

    private static boolean sensitiveNode(JsonNode node) {
        return node.path("writeOnly").asBoolean(false)
                || node.path("x-ui-options").path("sensitive").asBoolean(false);
    }

    private static boolean isArraySchema(JsonNode node) {
        return "array".equals(node.path("type").asText())
                || node.has("items")
                || node.has("prefixItems");
    }

    private static boolean containsAnnotation(JsonNode root,
                                              JsonNode node,
                                              boolean sensitive,
                                              Set<String> visitedReferences,
                                              int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            return false;
        }
        if (sensitive ? sensitiveNode(node) : node.path("readOnly").asBoolean(false)) {
            return true;
        }
        String reference = localReference(node);
        if (reference != null && visitedReferences.add(reference)) {
            JsonNode resolved = resolveReference(root, reference);
            if (containsAnnotation(root, resolved, sensitive, visitedReferences, depth + 1)) {
                return true;
            }
            visitedReferences.remove(reference);
        }
        for (JsonNode child : appliedChildren(node)) {
            if (containsAnnotation(root, child, sensitive,
                    visitedReferences, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotationOnPath(JsonNode root,
                                               String pointer,
                                               boolean sensitive) {
        List<JsonNode> current = List.of(root);
        if (hasDirectAnnotation(root, current, sensitive)) {
            return true;
        }
        for (String segment : segments(pointer)) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                collectPropertyCandidates(root, node, segment, next,
                        new LinkedHashSet<>(), 0);
            }
            if (next.isEmpty()) {
                return false;
            }
            if (hasDirectAnnotation(root, next, sensitive)) {
                return true;
            }
            current = distinct(next);
        }
        return false;
    }

    private static boolean hasDirectAnnotation(JsonNode root,
                                               Collection<JsonNode> nodes,
                                               boolean sensitive) {
        return nodes.stream().anyMatch(node -> hasDirectAnnotation(
                root, node, sensitive, new LinkedHashSet<>(), 0));
    }

    private static boolean hasDirectAnnotation(JsonNode root,
                                               JsonNode node,
                                               boolean sensitive,
                                               Set<String> referenceStack,
                                               int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            return false;
        }
        if (sensitive ? sensitiveNode(node) : node.path("readOnly").asBoolean(false)) {
            return true;
        }
        String reference = localReference(node);
        if (reference != null && referenceStack.add(reference)) {
            if (hasDirectAnnotation(root, resolveReference(root, reference), sensitive,
                    referenceStack, depth + 1)) {
                return true;
            }
            referenceStack.remove(reference);
        }
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                for (JsonNode schema : schemas) {
                    if (hasDirectAnnotation(root, schema, sensitive,
                            referenceStack, depth + 1)) {
                        return true;
                    }
                }
            }
        }
        for (String keyword : List.of("if", "then", "else")) {
            JsonNode conditional = node.path(keyword);
            if (conditional.isObject()
                    && hasDirectAnnotation(root, conditional, sensitive,
                    referenceStack, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRestartAnnotation(JsonNode root,
                                                JsonNode node,
                                                Set<String> referenceStack,
                                                int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            return false;
        }
        if (node.path("x-ui-options").path("restartRequired").asBoolean(false)) {
            return true;
        }
        String reference = localReference(node);
        if (reference != null && referenceStack.add(reference)) {
            if (hasRestartAnnotation(root, resolveReference(root, reference),
                    referenceStack, depth + 1)) {
                return true;
            }
            referenceStack.remove(reference);
        }
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                for (JsonNode schema : schemas) {
                    if (hasRestartAnnotation(root, schema, referenceStack, depth + 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<JsonNode> nodesAt(JsonNode root, String pointer) {
        List<JsonNode> current = List.of(root);
        for (String segment : segments(pointer)) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                collectPropertyCandidates(root, node, segment, next,
                        new LinkedHashSet<>(), 0);
            }
            current = distinct(next);
            if (current.isEmpty()) {
                break;
            }
        }
        return current;
    }

    private static void collectPropertyCandidates(JsonNode root,
                                                  JsonNode node,
                                                  String property,
                                                  List<JsonNode> result,
                                                  Set<String> referenceStack,
                                                  int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            return;
        }
        boolean matched = false;
        JsonNode exact = node.path("properties").path(property);
        if (!exact.isMissingNode()) {
            result.add(exact);
            matched = true;
        }
        JsonNode patterns = node.path("patternProperties");
        if (patterns.isObject()) {
            var fields = patterns.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                try {
                    if (Pattern.compile(entry.getKey()).matcher(property).find()) {
                        result.add(entry.getValue());
                        matched = true;
                    }
                } catch (PatternSyntaxException ignored) {
                    // Invalid patterns are rejected by schema validation; keep traversal fail-safe.
                }
            }
        }
        if (!matched) {
            JsonNode additional = node.path("additionalProperties");
            if (additional.isObject()) {
                result.add(additional);
            }
            JsonNode unevaluated = node.path("unevaluatedProperties");
            if (unevaluated.isObject()) {
                result.add(unevaluated);
            }
        }

        String reference = localReference(node);
        if (reference != null && referenceStack.add(reference)) {
            collectPropertyCandidates(root, resolveReference(root, reference), property,
                    result, referenceStack, depth + 1);
            referenceStack.remove(reference);
        }
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                for (JsonNode schema : schemas) {
                    collectPropertyCandidates(root, schema, property, result,
                            referenceStack, depth + 1);
                }
            }
        }
        for (String keyword : CONDITIONAL_KEYWORDS) {
            JsonNode nested = node.path(keyword);
            if (nested.isObject()) {
                if ("dependentSchemas".equals(keyword)) {
                    for (JsonNode schema : nested) {
                        collectPropertyCandidates(root, schema, property, result,
                                referenceStack, depth + 1);
                    }
                } else {
                    collectPropertyCandidates(root, nested, property, result,
                            referenceStack, depth + 1);
                }
            }
        }
    }

    private static void collectApplicableNodes(JsonNode root,
                                               JsonNode node,
                                               List<JsonNode> result,
                                               Set<String> referenceStack,
                                               int depth) {
        if (node == null || node.isMissingNode() || depth > MAX_SCHEMA_DEPTH) {
            return;
        }
        result.add(node);
        String reference = localReference(node);
        if (reference != null && referenceStack.add(reference)) {
            collectApplicableNodes(root, resolveReference(root, reference), result,
                    referenceStack, depth + 1);
            referenceStack.remove(reference);
        }
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                for (JsonNode schema : schemas) {
                    collectApplicableNodes(root, schema, result, referenceStack, depth + 1);
                }
            }
        }
    }

    private static List<JsonNode> appliedChildren(JsonNode node) {
        List<JsonNode> children = new ArrayList<>();
        addObjectValues(node.path("properties"), children);
        addObjectValues(node.path("patternProperties"), children);
        addObjectValues(node.path("dependentSchemas"), children);
        for (String keyword : COMPOSITION_KEYWORDS) {
            JsonNode schemas = node.path(keyword);
            if (schemas.isArray()) {
                schemas.forEach(children::add);
            }
        }
        JsonNode prefixItems = node.path("prefixItems");
        if (prefixItems.isArray()) {
            prefixItems.forEach(children::add);
        }
        for (String keyword : List.of("if", "then", "else", "items", "contains",
                "additionalProperties", "unevaluatedProperties")) {
            JsonNode child = node.path(keyword);
            if (child.isObject()) {
                children.add(child);
            }
        }
        return children;
    }

    private static void addObjectValues(JsonNode object, List<JsonNode> target) {
        if (object.isObject()) {
            object.forEach(target::add);
        }
    }

    private static List<JsonNode> distinct(List<JsonNode> nodes) {
        return new ArrayList<>(new LinkedHashSet<>(nodes));
    }

    private static String localReference(JsonNode node) {
        JsonNode reference = node.path("$ref");
        return reference.isTextual() && reference.asText().startsWith("#")
                ? reference.asText()
                : null;
    }

    private static JsonNode resolveReference(JsonNode root, String reference) {
        JsonNode resolved = root.at(reference.substring(1));
        return resolved.isMissingNode() ? MissingNode.getInstance() : resolved;
    }

    private static List<String> segments(String pointer) {
        if (pointer == null || pointer.isBlank()) {
            return List.of();
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("配置字段路径必须是 JSON Pointer");
        }
        String[] raw = pointer.substring(1).split("/", -1);
        List<String> result = new ArrayList<>(raw.length);
        for (String segment : raw) {
            result.add(segment.replace("~1", "/").replace("~0", "~"));
        }
        return result;
    }

    private static List<String> pointerPrefixes(String pointer) {
        List<String> parts = segments(pointer);
        List<String> result = new ArrayList<>(parts.size());
        StringBuilder prefix = new StringBuilder();
        for (String part : parts) {
            prefix.append('/').append(escape(part));
            result.add(prefix.toString());
        }
        return result;
    }

    private static void requireFieldPointer(String pointer, List<String> segments) {
        if (segments.isEmpty() || segments.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("配置字段路径不能为空: " + pointer);
        }
    }

    private static String escape(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return stringMap(map);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            collection.forEach(item -> result.add(copyValue(item)));
            return result;
        }
        return value;
    }

    private static Map<String, Object> stringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), copyValue(value)));
        return result;
    }
}
