package com.nexusadmin.api.exception;

import com.nexusadmin.api.domain.result.ProblemDetail;
import com.nexusadmin.api.domain.result.StatusCodes;
import com.nexusadmin.core.exception.CoreException;
import com.nexusadmin.core.exception.DescriptorParseException;
import com.nexusadmin.core.exception.DomainException;
import com.nexusadmin.core.exception.ExtensionNotFoundException;
import com.nexusadmin.core.exception.PluginDescriptorException;
import com.nexusadmin.core.exception.PluginException;
import com.nexusadmin.core.exception.PluginLoadException;
import com.nexusadmin.core.exception.PluginSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.List;

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
                        fe.getField(),
                        fe.getRejectedValue(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "校验失败"
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
}
