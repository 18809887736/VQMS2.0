package com.ruoyi.vqms.source;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

/**
 * 外部源数据源配置（与 RuoYi 主库 Druid 完全隔离；只读小池）。
 *
 * application.yml:
 *   vqms.source.jdbc.url / username / password / driver-class-name
 * 部署环境变量：VQMS_SOURCE_JDBC_URL / VQMS_SOURCE_JDBC_USERNAME / VQMS_SOURCE_JDBC_PASSWORD
 */
@Configuration
@EnableConfigurationProperties(SourceJdbcProperties.class)
public class SourceDataSourceConfig {

    @Bean
    public DataSource vqmsSourceDataSource(SourceJdbcProperties p) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("vqms-source");
        ds.setMaximumPoolSize(2);
        ds.setReadOnly(true);
        if (p.getUrl() != null && !p.getUrl().isBlank()) {
            ds.setJdbcUrl(p.getUrl());
            ds.setUsername(p.getUsername());
            ds.setPassword(p.getPassword());
            ds.setDriverClassName(p.getDriverClassName());
        }
        // url 未配置：连接惰性建立，首笔查询报 jdbcUrl required（fail-fast，不阻断应用启动）
        return ds;
    }

    @Bean("sourceJdbcTemplate")
    public JdbcTemplate sourceJdbcTemplate(DataSource vqmsSourceDataSource) {
        JdbcTemplate t = new JdbcTemplate(vqmsSourceDataSource);
        t.setQueryTimeout(120);
        t.setMaxRows(1_000_000);
        return t;
    }
}
