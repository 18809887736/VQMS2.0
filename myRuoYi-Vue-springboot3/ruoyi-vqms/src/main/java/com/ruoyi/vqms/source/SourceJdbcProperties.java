package com.ruoyi.vqms.source;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部源连接配置（application.yml vqms.source.jdbc.* / 环境变量 VQMS_SOURCE_JDBC_*）。
 * 口令不入受跟踪文件：未配置时首笔查询 fail-fast（Hikari jdbcUrl required）。
 */
@ConfigurationProperties(prefix = "vqms.source.jdbc")
public class SourceJdbcProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
}
