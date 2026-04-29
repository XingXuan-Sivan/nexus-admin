package com.nexusadmin.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 引导认证配置属性。
 * <p>
 * 提供系统初始化时的默认管理员认证凭据，属于 API 层专有配置。
 * 当存在其他认证提供者时，引导认证将被自动禁用。
 * <p>
 * <strong>配置项（platform.auth.bootstrap 前缀）：</strong>
 * <ul>
 *   <li>platform.auth.bootstrap.username - 管理员用户名，默认 admin</li>
 *   <li>platform.auth.bootstrap.password - 管理员密码，默认 admin123</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "platform.auth.bootstrap")
public class BootstrapAuthProperties {

    /**
     * 管理员用户名。
     */
    private String username = "admin";

    /**
     * 管理员密码。
     */
    private String password = "admin123";

    /**
     * 获取管理员用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置管理员用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取管理员密码。
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置管理员密码。
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
