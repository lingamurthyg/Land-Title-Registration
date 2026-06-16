package com.trianz.ltr.config;

import com.trianz.ltr.util.WASTransactionUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * CloudConfiguration - Spring Boot configuration for AWS cloud deployment.
 *
 * CLOUD-NATIVE FEATURES:
 *   - HikariCP DataSource with AWS Secrets Manager credentials
 *   - Redis session management via Amazon ElastiCache
 *   - Spring transaction management (replaces EJB CMT)
 *   - UTC timezone standardization
 *
 * AWS SERVICES INTEGRATION:
 *   - AWS Secrets Manager for database credentials
 *   - Amazon RDS/Aurora for PostgreSQL database
 *   - Amazon ElastiCache (Redis) for session state
 *   - AWS Application Load Balancer for traffic distribution
 */
@Configuration
@EnableTransactionManagement
@EnableRedisHttpSession
public class CloudConfiguration {

    /**
     * Configure HikariCP DataSource with credentials from AWS Secrets Manager.
     * Replaces WebSphere JNDI DataSource lookup.
     */
    @Bean
    public DataSource dataSource() {
        // Retrieve credentials from AWS Secrets Manager
        WASTransactionUtil.DatabaseCredentials creds = WASTransactionUtil.getDatabaseCredentials();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(creds.getJdbcUrl());
        config.setUsername(creds.getUsername());
        config.setPassword(creds.getPassword());

        // HikariCP optimizations for AWS RDS
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("LandTitleRegistryPool");

        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }
}
