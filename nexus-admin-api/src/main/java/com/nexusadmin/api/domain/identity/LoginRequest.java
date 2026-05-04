package com.nexusadmin.api.domain.identity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录请求参数。
 *
 * @param username 用户名
 * @param password 密码
 */
@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,
        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
        String password) {
}
