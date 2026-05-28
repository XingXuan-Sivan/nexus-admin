package com.nexusadmin.api.domain.ai;

/**
 * MCP 客户端连接配置视图对象，用于管理面板 CRUD 和数据持久化。
 *
 * <p>封装一个外部 MCP 服务端的连接参数与运行时状态信息。</p>
 */
public class McpConnectionInfo {

    /** 连接唯一标识 */
    private String id;

    /** 显示名称 */
    private String name;

    /** MCP 服务端地址 */
    private String url;

    /** 传输协议（默认 "http"） */
    private String protocol;

    /** 认证令牌 */
    private String authToken;

    /** 是否启用 */
    private boolean enabled;

    /** 连接状态：CONNECTED / DISCONNECTED / ERROR */
    private String status;

    /** 是否将远程工具桥接为平台 AiTool（默认 true） */
    private boolean bridgeEnabled = true;

    /** 桥接模式：ALL（全部工具）/ SELECTED（仅选中）/ NONE（不桥接） */
    private String bridgeMode = "ALL";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBridgeEnabled() {
        return bridgeEnabled;
    }

    public void setBridgeEnabled(boolean bridgeEnabled) {
        this.bridgeEnabled = bridgeEnabled;
    }

    public String getBridgeMode() {
        return bridgeMode;
    }

    public void setBridgeMode(String bridgeMode) {
        this.bridgeMode = bridgeMode;
    }

    /** 根据启用状态和连接状态，获取综合显示状态。 */
    public String getDisplayStatus() {
        if (!enabled) {
            return "DISABLED";
        }
        return status != null ? status : "DISCONNECTED";
    }
}
