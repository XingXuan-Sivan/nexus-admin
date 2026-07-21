package com.nexusadmin.core.exception;

/** 配置写入时检测到客户端 revision 已过期。 */
public final class ConfigRevisionConflictException extends CoreException {

    private final String expectedRevision;
    private final String currentRevision;

    public ConfigRevisionConflictException(String expectedRevision, String currentRevision) {
        super("配置已被其他会话修改，请重新加载后再提交");
        this.expectedRevision = expectedRevision;
        this.currentRevision = currentRevision;
    }

    public String expectedRevision() {
        return expectedRevision;
    }

    public String currentRevision() {
        return currentRevision;
    }
}
