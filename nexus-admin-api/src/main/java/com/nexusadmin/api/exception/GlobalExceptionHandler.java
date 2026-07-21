package com.nexusadmin.api.exception;

import com.nexusadmin.api.domain.result.ProblemDetail;
import com.nexusadmin.api.domain.result.StatusCodes;
import com.nexusadmin.api.configuration.ConfigDocumentLockedException;
import com.nexusadmin.api.configuration.ConfigDocumentTooLargeException;
import com.nexusadmin.api.configuration.ConfigDomainNotFoundException;
import com.nexusadmin.api.configuration.ConfigValidationException;
import com.nexusadmin.api.configuration.ConfigPermissionDeniedException;
import com.nexusadmin.core.exception.CoreException;
import com.nexusadmin.core.exception.DescriptorParseException;
import com.nexusadmin.core.exception.DomainException;
import com.nexusadmin.core.exception.ExtensionNotFoundException;
import com.nexusadmin.core.exception.PluginDescriptorException;
import com.nexusadmin.core.exception.PluginException;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.exception.PluginSourceException;
import com.nexusadmin.core.exception.ConfigRevisionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 全局异常处理器，将各类异常统一转换为 RFC 7807 ProblemDetail 格式响应。
 *
 * <p>处理优先级从高到低：插件操作异常 → 核心领域异常 → 参数校验异常 → 权限异常 → 通用异常。</p>
 *
 * <p>type URI 格式：{@code https://nexusadmin.io/probs/{category}/{problem}}</p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TYPE_BASE = "https://nexusadmin.io/probs/";
    private static final String CONFIG_TYPE_BASE = "urn:nexus-admin:problem:";
    private static final String CONFIG_DOMAIN_NOT_FOUND = "CONFIG_DOMAIN_NOT_FOUND";
    private static final String CONFIG_REVISION_CONFLICT = "CONFIG_REVISION_CONFLICT";
    private static final String CONFIG_VALIDATION_FAILED = "CONFIG_VALIDATION_FAILED";
    private static final String CONFIG_DOCUMENT_LOCKED = "CONFIG_DOCUMENT_LOCKED";
    private static final String CONFIG_DOCUMENT_TOO_LARGE = "CONFIG_DOCUMENT_TOO_LARGE";
    private static final String CONFIG_PERMISSION_DENIED = "CONFIG_PERMISSION_DENIED";

    @ExceptionHandler(ConfigPermissionDeniedException.class)
    public ResponseEntity<ProblemDetail> handleConfigPermissionDenied(ConfigPermissionDeniedException ex,
                                                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(CONFIG_TYPE_BASE + "config-permission-denied")
                .title("配置操作权限不足")
                .status(HttpStatus.FORBIDDEN.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .scopeId(configScopeId(request))
                .errorCode(CONFIG_PERMISSION_DENIED)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConfigDomainNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleConfigDomainNotFound(ConfigDomainNotFoundException ex,
                                                                     HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(CONFIG_TYPE_BASE + "config-domain-not-found")
                .title("配置域不存在")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .scopeId(configScopeId(request))
                .errorCode(CONFIG_DOMAIN_NOT_FOUND)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConfigRevisionConflictException.class)
    public ResponseEntity<ProblemDetail> handleConfigRevisionConflict(ConfigRevisionConflictException ex,
                                                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(CONFIG_TYPE_BASE + "config-revision-conflict")
                .title("配置版本冲突")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .scopeId(configScopeId(request))
                .errorCode(CONFIG_REVISION_CONFLICT)
                .currentRevision(ex.currentRevision())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConfigValidationException.class)
    public ResponseEntity<ProblemDetail> handleConfigValidation(ConfigValidationException ex,
                                                                 HttpServletRequest request) {
        List<ProblemDetail.FieldError> fieldErrors = ex.issues().stream()
                .map(issue -> new ProblemDetail.FieldError(
                        issue.path(),
                        issue.keyword(),
                        issue.messageKey(),
                        issue.params(),
                        issue.line(),
                        issue.column()))
                .toList();
        ProblemDetail problem = ProblemDetail.builder()
                .type(CONFIG_TYPE_BASE + "config-validation")
                .title("配置校验失败")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .scopeId(configScopeId(request))
                .errorCode(CONFIG_VALIDATION_FAILED)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.unprocessableEntity()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConfigDocumentLockedException.class)
    public ResponseEntity<ProblemDetail> handleConfigDocumentLocked(ConfigDocumentLockedException ex,
                                                                     HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(CONFIG_TYPE_BASE + "config-document-locked")
                .title("配置文档已锁定")
                .status(HttpStatus.LOCKED.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .scopeId(configScopeId(request))
                .errorCode(CONFIG_DOCUMENT_LOCKED)
                .build();
        return ResponseEntity.status(HttpStatus.LOCKED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ConfigDocumentTooLargeException.class)
    public ResponseEntity<ProblemDetail> handleConfigDocumentTooLarge(ConfigDocumentTooLargeException ex,
                                                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(CONFIG_TYPE_BASE + "config-document-too-large")
                .title("配置文档过大")
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .detail(ex.getMessage())
                .instance(request.getRequestURI())
                .scopeId(configScopeId(request))
                .errorCode(CONFIG_DOCUMENT_TOO_LARGE)
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== 插件操作异常 ====================

    /**
     * 处理插件操作异常。
     *
     * @param ex 插件操作异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(PluginOperationException.class)
    public ResponseEntity<ProblemDetail> handlePluginOperationException(PluginOperationException ex) {
        log.warn("插件操作异常: {}", ex.getMessage());

        StatusCodes statusCode = mapOperationToStatus(ex.getOperation());
        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "plugin/operation-failed")
                .title("插件操作失败")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .errorCode(statusCode.code())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== 核心模块异常 ====================

    /**
     * 处理插件加载异常。
     *
     * @param ex 插件加载异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(PluginLoadException.class)
    public ResponseEntity<ProblemDetail> handlePluginLoadException(PluginLoadException ex) {
        log.error("插件加载异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "plugin/load-failed")
                .title("插件加载失败")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.PLUGIN_LOAD_FAILED.code())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理插件描述文件异常。
     *
     * @param ex 插件描述文件异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(PluginDescriptorException.class)
    public ResponseEntity<ProblemDetail> handlePluginDescriptorException(PluginDescriptorException ex) {
        log.error("插件描述文件异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "plugin/descriptor-invalid")
                .title("插件描述文件无效")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.PLUGIN_DESCRIPTOR_INVALID.code())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理描述文件解析异常。
     *
     * @param ex 描述文件解析异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(DescriptorParseException.class)
    public ResponseEntity<ProblemDetail> handleDescriptorParseException(DescriptorParseException ex) {
        log.error("描述文件解析异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "plugin/descriptor-invalid")
                .title("插件描述文件无效")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.PLUGIN_DESCRIPTOR_INVALID.code())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理插件源异常。
     *
     * @param ex 插件源异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(PluginSourceException.class)
    public ResponseEntity<ProblemDetail> handlePluginSourceException(PluginSourceException ex) {
        log.error("插件源异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "plugin/source-error")
                .title("插件源操作失败")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.PLUGIN_LOAD_FAILED.code())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理扩展未找到异常。
     *
     * @param ex 扩展未找到异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(ExtensionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleExtensionNotFoundException(ExtensionNotFoundException ex) {
        log.warn("扩展未找到: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "system/extension-not-found")
                .title("扩展点未找到")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.SYSTEM_UNAVAILABLE.code())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理领域异常。
     *
     * @param ex 领域异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
        log.warn("领域异常: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "business/domain-violation")
                .title("业务规则校验失败")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.BAD_REQUEST.code())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理功能未实现异常（骨架 Service 等待插件提供实现）。
     *
     * @param ex 功能未实现异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(ExtensionNotImplementedException.class)
    public ResponseEntity<ProblemDetail> handleExtensionNotImplementedException(ExtensionNotImplementedException ex) {
        log.warn("功能未实现，需安装插件: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "system/not-implemented")
                .title("功能未实现")
                .status(HttpStatus.NOT_IMPLEMENTED.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.SYSTEM_UNAVAILABLE.code())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理插件通用异常。
     *
     * @param ex 插件通用异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(PluginException.class)
    public ResponseEntity<ProblemDetail> handlePluginException(PluginException ex) {
        log.error("插件异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "plugin/error")
                .title("插件操作异常")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.PLUGIN_LOAD_FAILED.code())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理核心异常（兜底）。
     *
     * @param ex 核心异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ProblemDetail> handleCoreException(CoreException ex) {
        log.error("核心异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "system/core-error")
                .title("系统内部错误")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.INTERNAL_ERROR.code())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== 参数校验异常 ====================

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "request/invalid-argument")
                .title("请求参数错误")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .errorCode(StatusCodes.BAD_REQUEST.code())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 处理请求体校验异常。
     *
     * @param ex 校验异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("请求体校验失败: {}", ex.getMessage());

        List<ProblemDetail.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ProblemDetail.FieldError(
                        requestFieldPath(fe.getField()),
                        fe.getCode() == null ? "invalid" : fe.getCode().toLowerCase(Locale.ROOT),
                        "validation." + (fe.getCode() == null
                                ? "invalid"
                                : fe.getCode().toLowerCase(Locale.ROOT)),
                        Map.of(),
                        null,
                        null
                ))
                .toList();

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "request/validation-failed")
                .title("请求参数校验失败")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("请求参数不满足校验规则")
                .errorCode(StatusCodes.BAD_REQUEST.code())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== 权限异常 ====================

    /**
     * 处理访问拒绝异常。
     *
     * @param ex 访问拒绝异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("访问被拒绝: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "auth/access-denied")
                .title("无访问权限")
                .status(HttpStatus.FORBIDDEN.value())
                .detail(ex.getMessage() != null ? ex.getMessage() : "当前用户无权执行此操作")
                .errorCode(StatusCodes.PERMISSION_DENIED.code())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== Spring Web 异常 ====================

    /**
     * 不支持的媒体类型异常。
     *
     * @param ex 媒体类型不支持异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("不支持的 Content-Type: {}", ex.getContentType());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "request/unsupported-media-type")
                .title("不支持的请求类型")
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .detail("Content-Type '" + ex.getContentType() + "' 不被支持，支持的格式: " + ex.getSupportedMediaTypes())
                .errorCode(StatusCodes.BAD_REQUEST.code())
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * 静态资源未找到异常。
     *
     * @param ex 资源未找到异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("静态资源未匹配: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "resource/not-found")
                .title("资源不存在")
                .status(HttpStatus.NOT_FOUND.value())
                .detail("请求的资源不存在")
                .errorCode(StatusCodes.NOT_FOUND.code())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== 通用异常 ====================

    /**
     * 处理未捕获的通用异常。
     *
     * @param ex 通用异常
     * @return ProblemDetail 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception ex) {
        log.error("未处理的异常: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.builder()
                .type(TYPE_BASE + "system/internal-error")
                .title("服务器内部错误")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("服务器处理请求时发生内部错误")
                .errorCode(StatusCodes.INTERNAL_ERROR.code())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 根据插件操作类型映射状态码。
     *
     * @param operation 操作类型
     * @return 对应的状态码
     */
    private StatusCodes mapOperationToStatus(PluginOperationException.Operation operation) {
        return switch (operation) {
            case START, STOP -> StatusCodes.PLUGIN_STATE_INVALID;
            case ENABLE, DISABLE -> StatusCodes.PLUGIN_STATE_INVALID;
            case LOAD -> StatusCodes.PLUGIN_LOAD_FAILED;
            case UNLOAD -> StatusCodes.PLUGIN_NOT_FOUND;
        };
    }

    private String configScopeId(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attribute instanceof Map<?, ?> variables) {
            Object scopeId = variables.get("scopeId");
            return scopeId == null ? null : scopeId.toString();
        }
        return null;
    }

    private String requestFieldPath(String field) {
        if (field == null || field.isBlank()) {
            return "";
        }
        return "/" + field.replace("~", "~0")
                .replace("/", "~1")
                .replace(".", "/");
    }
}
