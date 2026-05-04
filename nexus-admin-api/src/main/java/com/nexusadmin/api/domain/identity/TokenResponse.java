package com.nexusadmin.api.domain.identity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Token 响应数据。
 *
 * @param accessToken  访问令牌
 * @param refreshToken 刷新令牌
 * @param expiresIn    过期时间（秒）
 * @param tokenType    令牌类型
 */
@Schema(description = "Token 响应")
public record TokenResponse(
        @Schema(description = "访问令牌")
        String accessToken,
        @Schema(description = "刷新令牌")
        String refreshToken,
        @Schema(description = "过期时间（秒）")
        long expiresIn,
        @Schema(description = "令牌类型")
        String tokenType) {
}
