package com.nexusadmin.core.plugin.resolve.impl;

import com.nexusadmin.core.plugin.resolve.VersionManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认版本管理器实现。
 * <p>支持语义化版本（SemVer）格式的版本比较和兼容性检查。</p>
 */
public class DefaultVersionManager implements VersionManager {

    /**
     * 语义化版本正则表达式。
     */
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*))?(?:\\+([a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*))?$"
    );

    @Override
    public boolean isCompatible(String coreVersion, String required) {
        if (required == null || required.isBlank()) {
            // 无版本要求，视为兼容
            return true;
        }
        if (coreVersion == null || coreVersion.isBlank()) {
            return false;
        }

        String trimmed = required.trim();

        // 处理 ^x.y.z 格式（兼容相同主版本）
        if (trimmed.startsWith("^")) {
            String ver = trimmed.substring(1).trim();
            return isCompatibleWithCaret(coreVersion, ver);
        }

        // 处理 ~x.y.z 格式（兼容相同次版本）
        if (trimmed.startsWith("~")) {
            String ver = trimmed.substring(1).trim();
            return isCompatibleWithTilde(coreVersion, ver);
        }

        // 处理 >=x.y.z 格式
        if (trimmed.startsWith(">=")) {
            String ver = trimmed.substring(2).trim();
            return compare(coreVersion, ver) >= 0;
        }

        // 处理 >x.y.z 格式
        if (trimmed.startsWith(">")) {
            String ver = trimmed.substring(1).trim();
            return compare(coreVersion, ver) > 0;
        }

        // 处理 <=x.y.z 格式
        if (trimmed.startsWith("<=")) {
            String ver = trimmed.substring(2).trim();
            return compare(coreVersion, ver) <= 0;
        }

        // 处理 <x.y.z 格式
        if (trimmed.startsWith("<")) {
            String ver = trimmed.substring(1).trim();
            return compare(coreVersion, ver) < 0;
        }

        // 精确匹配
        return compare(coreVersion, trimmed) == 0;
    }

    /**
     * 检查 ^ 兼容性（相同主版本）。
     */
    private boolean isCompatibleWithCaret(String coreVersion, String required) {
        int[] core = parseVersion(coreVersion);
        int[] req = parseVersion(required);

        if (core == null || req == null) {
            return false;
        }

        // ^0.x.y 只兼容补丁版本
        if (req[0] == 0) {
            return core[0] == 0 && core[1] == req[1] && core[2] >= req[2];
        }

        // ^x.y.z 兼容相同主版本
        return core[0] == req[0] &&
                (core[1] > req[1] || (core[1] == req[1] && core[2] >= req[2]));
    }

    /**
     * 检查 ~ 兼容性（相同次版本）。
     */
    private boolean isCompatibleWithTilde(String coreVersion, String required) {
        int[] core = parseVersion(coreVersion);
        int[] req = parseVersion(required);

        if (core == null || req == null) {
            return false;
        }

        return core[0] == req[0] &&
                core[1] == req[1] &&
                core[2] >= req[2];
    }

    @Override
    public int compare(String version1, String version2) {
        int[] v1 = parseVersion(version1);
        int[] v2 = parseVersion(version2);

        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("非法的版本号格式");
        }

        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(v1[i], v2[i]);
            if (cmp != 0) {
                return cmp;
            }
        }

        return 0;
    }

    @Override
    public boolean isValid(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        return SEMVER_PATTERN.matcher(version.trim()).matches();
    }

    /**
     * 解析版本号字符串为整数数组 [主版本, 次版本, 补丁版本]。
     *
     * @param version 版本号字符串
     * @return 版本号整数数组，解析失败返回 null
     */
    private int[] parseVersion(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }

        Matcher matcher = SEMVER_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return null;
        }

        return new int[]{
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        };
    }
}
