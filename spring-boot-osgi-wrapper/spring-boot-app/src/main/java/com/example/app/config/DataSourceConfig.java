package com.example.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * DataSource configuration that supports:
 * 1. External DataSource injection (from OSGi wrapper)
 * 2. Standalone HikariCP DataSource creation
 *
 * When app.datasource.external=true, uses the externally provided DataSource.
 * Otherwise, creates a new HikariCP DataSource based on application properties.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${app.datasource.external:false}")
    private boolean useExternalDataSource;

    @Value("${spring.datasource.url:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

    /**
     * Primary DataSource bean.
     * Uses external DataSource if provided, otherwise creates HikariCP DataSource.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        if (useExternalDataSource) {
            return getExternalDataSource();
        }
        return createHikariDataSource();
    }

    /**
     * Retrieves the externally provided DataSource (registered by ApplicationLauncher).
     */
    private DataSource getExternalDataSource() {
        try {
            if (applicationContext.containsBean("externalDataSource")) {
                DataSource external = applicationContext.getBean("externalDataSource", DataSource.class);
                logger.info("Using external DataSource from OSGi service registry: {}", external.getClass().getName());
                return external;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve external DataSource", e);
        }

        logger.warn("External DataSource not found, falling back to local DataSource");
        return createHikariDataSource();
    }

    /**
     * Creates a standalone HikariCP DataSource.
     */
    private DataSource createHikariDataSource() {
        logger.info("Creating local HikariCP DataSource");
        logger.info("JDBC URL: {}", jdbcUrl);
        logger.info("Driver: {}", driverClassName);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        // Connection pool settings
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setIdleTimeout(30000);
        dataSource.setConnectionTimeout(30000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setPoolName("SpringBootOsgiPool");

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
