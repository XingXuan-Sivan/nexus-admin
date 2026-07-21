package com.nexusadmin.api.configuration;

/** 配置文档超过服务端大小限制。 */
public final class ConfigDocumentTooLargeException extends RuntimeException {

    private final int actualBytes;
    private final int maxBytes;

    public ConfigDocumentTooLargeException(int actualBytes, int maxBytes) {
        super("配置文档大小为 " + actualBytes + " 字节，超过 " + maxBytes + " 字节限制");
        this.actualBytes = actualBytes;
        this.maxBytes = maxBytes;
    }

    public int actualBytes() {
        return actualBytes;
    }

    public int maxBytes() {
        return maxBytes;
    }
}
