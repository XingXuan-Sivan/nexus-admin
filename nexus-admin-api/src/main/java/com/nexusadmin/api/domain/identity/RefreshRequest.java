package com.nexusadmin.api.domain.identity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 刷新 Token 请求参数。
 *
 * @param refreshToken 刷新令牌
 */
@Schema(description = "刷新 Token 请求")
public record RefreshRequest(
        @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken) {
}
