package com.trianz.ltr.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * DataSource Configuration with HikariCP Connection Pool
 *
 * Replaces WAS JNDI DataSource lookup with HikariCP connection pool.
 * Provides high-performance connection pooling for AWS RDS.
 *
 * Cloud-Ready Features:
 * - HikariCP connection pooling (fastest JDBC pool)
 * - Connection leak detection
 * - Automatic connection validation
 * - Configurable pool sizing for cloud scaling
 * - Connection timeout handling
 * - Health check integration
 *
 * HikariCP Benefits:
 * - Zero-overhead connection pooling
 * - Fast connection acquisition
 * - Reliable connection validation
 * - Excellent monitoring and metrics
 * - Production-proven reliability
 *
 * AWS RDS Optimization:
 * - Connection pooling reduces RDS connection overhead
 * - Configurable pool size for cost optimization
 * - Connection validation prevents stale connections
 * - Leak detection prevents connection exhaustion
 *
 * @author Cloud Migration Team
 * @version 2.0.0-cloud
 */
@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:600000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * Configure HikariCP DataSource
     * Replaces WAS JNDI DataSource with connection pool
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Configuring HikariCP DataSource for AWS RDS");
        logger.info("JDBC URL: {}", maskPassword(jdbcUrl));
        logger.info("Pool Configuration - Min: {}, Max: {}", minimumIdle, maximumPoolSize);

        HikariConfig config = new HikariConfig();
        
        // Basic configuration
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        // Pool sizing
        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);
        
        // Connection timeouts
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // Leak detection
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // Pool name for monitoring
        config.setPoolName("LandTitleHikariPool");
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Register JMX MBeans for monitoring
        config.setRegisterMbeans(true);
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        logger.info("HikariCP DataSource configured successfully");
        
        return dataSource;
    }

    /**
     * Mask password in JDBC URL for logging
     */
    private String maskPassword(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}
