package com.example.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * DataSource configuration that supports both JNDI lookup and direct connection.
 *
 * This configuration automatically detects the runtime environment:
 * - In Karaf OSGi: Uses JNDI to lookup DataSource from OSGi service registry
 * - Standalone: Creates a HikariCP DataSource directly
 */
@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.jndi-name:}")
    private String jndiName;

    @Value("${spring.datasource.url:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

    /**
     * Primary DataSource bean that attempts JNDI lookup first, falls back to direct connection.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        // Try JNDI lookup first if configured
        if (jndiName != null && !jndiName.isEmpty()) {
            DataSource jndiDataSource = lookupJndiDataSource();
            if (jndiDataSource != null) {
                logger.info("Successfully obtained DataSource from JNDI: {}", jndiName);
                return jndiDataSource;
            }
            logger.warn("JNDI lookup failed for {}, falling back to direct connection", jndiName);
        }

        // Fallback to direct HikariCP DataSource
        return createDirectDataSource();
    }

    /**
     * Attempts to lookup DataSource from JNDI context.
     * Works in both standard servlet containers and OSGi environments.
     */
    private DataSource lookupJndiDataSource() {
        try {
            // First try standard JNDI lookup
            Context initContext = new InitialContext();

            // Try different JNDI name patterns
            String[] jndiPatterns = {
                jndiName,
                "java:comp/env/" + jndiName,
                "osgi:service/" + jndiName,
                "java:/" + jndiName
            };

            for (String pattern : jndiPatterns) {
                try {
                    Object lookup = initContext.lookup(pattern);
                    if (lookup instanceof DataSource) {
                        logger.info("Found DataSource at JNDI location: {}", pattern);
                        return (DataSource) lookup;
                    }
                } catch (NamingException e) {
                    logger.debug("JNDI lookup failed for pattern: {}", pattern);
                }
            }
        } catch (NamingException e) {
            logger.debug("Could not create InitialContext: {}", e.getMessage());
        }

        // Try OSGi service lookup if available
        return lookupOsgiDataSource();
    }

    /**
     * Attempts to lookup DataSource from OSGi service registry.
     * This is specific to Karaf/OSGi environments.
     */
    private DataSource lookupOsgiDataSource() {
        try {
            // Use reflection to avoid compile-time OSGi dependency
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");
            Object bundle = frameworkUtilClass.getMethod("getBundle", Class.class)
                    .invoke(null, this.getClass());

            if (bundle != null) {
                Object bundleContext = bundle.getClass().getMethod("getBundleContext")
                        .invoke(bundle);

                if (bundleContext != null) {
                    // Lookup DataSource service
                    Object serviceRef = bundleContext.getClass()
                            .getMethod("getServiceReference", Class.class)
                            .invoke(bundleContext, DataSource.class);

                    if (serviceRef != null) {
                        Object service = bundleContext.getClass()
                                .getMethod("getService", Class.forName("org.osgi.framework.ServiceReference"))
                                .invoke(bundleContext, serviceRef);

                        if (service instanceof DataSource) {
                            logger.info("Found DataSource from OSGi service registry");
                            return (DataSource) service;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("OSGi service lookup not available: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Creates a direct HikariCP DataSource for standalone deployment.
     */
    private DataSource createDirectDataSource() {
        logger.info("Creating direct HikariCP DataSource");

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

        // Pool name for monitoring
        dataSource.setPoolName("SpringBootKarafPool");

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
