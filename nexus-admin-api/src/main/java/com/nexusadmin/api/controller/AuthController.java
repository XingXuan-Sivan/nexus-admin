package com.nexusadmin.api.controller;

import com.nexusadmin.api.auth.CompositeAuthProvider;
import com.nexusadmin.api.auth.RequirePermission;
import com.nexusadmin.api.context.InvocationContext;
import com.nexusadmin.api.domain.identity.CurrentUserInfo;
import com.nexusadmin.api.domain.identity.LoginRequest;
import com.nexusadmin.api.domain.identity.RefreshRequest;
import com.nexusadmin.api.domain.identity.TokenResponse;
import com.nexusadmin.api.domain.result.DataResult;
import com.nexusadmin.api.domain.result.Result;
import com.nexusadmin.api.domain.result.StatusCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nexusadmin.api.util.HttpAuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 * <p>
 * 提供登录、登出、刷新 Token、获取当前用户等认证相关 API。
 * 作为认证 API 的编排层，所有具体认证逻辑均委托给 {@link CompositeAuthProvider}。
 */
@RestController
@RequestMapping("/admin/v1/auth")
@Tag(name = "认证管理")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final CompositeAuthProvider authProvider;

    /**
     * 构造认证控制器。
     *
     * @param authProvider 组合认证提供者
     */
    public AuthController(CompositeAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return Token 响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public DataResult<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse response = authProvider.login(request, buildContext());
        if (response == null) {
            return DataResult.of(StatusCodes.AUTHENTICATION_FAILED, (TokenResponse) null);
        }
        return DataResult.success(response);
    }

    /**
     * 用户登出。
     *
     * @param httpRequest HTTP 请求
     * @return 操作结果
     */
    @PostMapping("/logout")
    @RequirePermission("*")
    @Operation(summary = "用户登出")
    public Result logout(HttpServletRequest httpRequest) {
        String token = HttpAuthUtils.extractBearerToken(httpRequest);
        if (token != null) {
            authProvider.logout(token, buildContext());
        }
        return Result.success();
    }

    /**
     * 刷新访问令牌。
     *
     * @param request 刷新请求
     * @return 新的 Token 响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public DataResult<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        TokenResponse response = authProvider.refresh(request.refreshToken(), buildContext());
        if (response == null) {
            return DataResult.of(StatusCodes.TOKEN_EXPIRED, (TokenResponse) null);
        }
        return DataResult.success(response);
    }

    /**
     * 获取当前登录用户信息。
     *
     * @param httpRequest HTTP 请求
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @RequirePermission("*")
    @Operation(summary = "获取当前用户信息")
    public DataResult<CurrentUserInfo> me(HttpServletRequest httpRequest) {
        String token = HttpAuthUtils.extractBearerToken(httpRequest);
        if (token == null) {
            return DataResult.of(StatusCodes.UNAUTHORIZED, (CurrentUserInfo) null);
        }
        CurrentUserInfo user = authProvider.getCurrentUser(token, buildContext());
        if (user == null) {
            return DataResult.of(StatusCodes.TOKEN_INVALID, (CurrentUserInfo) null);
        }
        return DataResult.success(user);
    }

    /**
     * 构建调用上下文。
     *
     * @return 调用上下文
     */
    private InvocationContext buildContext() {
        return InvocationContext.builder()
                .channelId("HTTP")
                .build();
    }

}
