package com.nexusadmin.api.configuration;

import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.domain.result.DataResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nexusadmin.api.configuration.ConfigModels.*;

/** 配置中心管理 API。 */
@RestController
@RequestMapping("/admin/v1/config")
@Tag(name = "配置中心")
public final class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/domains")
    @RequirePermission("config.view")
    @Operation(summary = "获取配置域目录")
    public DataResult<Catalog> listDomains() {
        return DataResult.success(configService.listDomains());
    }

    @GetMapping("/{scopeId}")
    @RequirePermission("config.view")
    @Operation(summary = "获取配置域快照")
    public ResponseEntity<DataResult<Snapshot>> getSnapshot(
            @PathVariable("scopeId") String scopeId) {
        Snapshot snapshot = configService.getSnapshot(scopeId);
        return withEtag(snapshot.revision(), DataResult.success(snapshot));
    }

    @GetMapping("/{scopeId}/schema")
    @RequirePermission("config.view")
    @Operation(summary = "获取原始 JSON Schema")
    public DataResult<SchemaDocument> getSchema(@PathVariable("scopeId") String scopeId) {
        return DataResult.success(configService.getSchema(scopeId));
    }

    @PostMapping("/{scopeId}/validate")
    @RequirePermission("config.manage")
    @Operation(summary = "校验结构化配置变更")
    public DataResult<ValidationResult> validate(
            @PathVariable("scopeId") String scopeId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String expectedRevision,
            @RequestBody ChangeRequest request) {
        return DataResult.success(configService.validateChanges(scopeId, request, expectedRevision));
    }

    @PutMapping("/{scopeId}")
    @RequirePermission("config.manage")
    @Operation(summary = "原子保存结构化配置变更")
    public ResponseEntity<DataResult<Snapshot>> update(
            @PathVariable("scopeId") String scopeId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String expectedRevision,
            @RequestBody ChangeRequest request) {
        Snapshot snapshot = configService.update(scopeId, request, expectedRevision);
        return withEtag(snapshot.revision(), DataResult.success(snapshot));
    }

    @PostMapping("/{scopeId}/reset")
    @RequirePermission("config.manage")
    @Operation(summary = "重置持久化配置覆盖")
    public ResponseEntity<DataResult<Snapshot>> reset(
            @PathVariable("scopeId") String scopeId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String expectedRevision,
            @RequestBody ResetRequest request) {
        Snapshot snapshot = configService.reset(scopeId, request, expectedRevision);
        return withEtag(snapshot.revision(), DataResult.success(snapshot));
    }

    @GetMapping("/{scopeId}/document")
    @RequirePermission("config.document.view")
    @Operation(summary = "读取受控配置文档")
    public ResponseEntity<DataResult<Document>> getDocument(
            @PathVariable("scopeId") String scopeId) {
        Document document = configService.getDocument(scopeId);
        return withEtag(document.revision(), DataResult.success(document));
    }

    @PostMapping("/{scopeId}/document/validate")
    @RequirePermission("config.document.manage")
    @Operation(summary = "校验受控配置文档")
    public DataResult<ValidationResult> validateDocument(
            @PathVariable("scopeId") String scopeId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String expectedRevision,
            @RequestBody DocumentRequest request) {
        return DataResult.success(configService.validateDocument(
                scopeId, request, expectedRevision));
    }

    @PutMapping("/{scopeId}/document")
    @RequirePermission("config.document.manage")
    @Operation(summary = "原子保存受控配置文档")
    public ResponseEntity<DataResult<DocumentSaveResult>> saveDocument(
            @PathVariable("scopeId") String scopeId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String expectedRevision,
            @RequestBody DocumentRequest request) {
        DocumentSaveResult result = configService.saveDocument(
                scopeId, request, expectedRevision);
        return withEtag(result.snapshot().revision(), DataResult.success(result));
    }

    private <T> ResponseEntity<T> withEtag(String revision, T body) {
        return ResponseEntity.ok()
                .eTag(revision)
                .body(body);
    }
}
