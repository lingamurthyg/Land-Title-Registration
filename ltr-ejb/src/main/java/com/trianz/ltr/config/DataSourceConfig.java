package com.trianz.ltr.config;

import com.trianz.ltr.util.CloudDataSourceUtil;
import com.trianz.ltr.util.CloudTransactionUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * DataSourceConfig - Spring configuration for cloud-native data source and transaction management.
 * 
 * Configures:
 * - HikariCP connection pool via CloudDataSourceUtil
 * - Spring transaction management
 * - AWS Secrets Manager integration for credentials
 */
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    /**
     * Configure the DataSource bean using CloudDataSourceUtil.
     * This provides HikariCP connection pooling with AWS Secrets Manager integration.
     */
    @Bean
    public DataSource dataSource() {
        return CloudDataSourceUtil.getDataSource();
    }

    /**
     * Configure Spring's transaction manager.
     * This replaces EJB Container-Managed Transactions (CMT).
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        
        // Set the transaction manager in CloudTransactionUtil for legacy code
        CloudTransactionUtil.setTransactionManager(txManager);
        
        return txManager;
    }
}
