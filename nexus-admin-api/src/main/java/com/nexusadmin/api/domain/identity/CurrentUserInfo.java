package com.nexusadmin.api.domain.identity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * 当前登录用户信息。
 *
 * @param userId      用户标识
 * @param username    用户名
 * @param displayName 显示名称
 * @param roles       角色集合
 * @param permissions 权限集合
 */
@Schema(description = "当前用户信息")
public record CurrentUserInfo(
        @Schema(description = "用户标识")
        String userId,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "显示名称")
        String displayName,
        @Schema(description = "角色集合")
        Set<String> roles,
        @Schema(description = "权限集合")
        Set<String> permissions) {
}
