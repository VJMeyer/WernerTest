package com.example.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Programmatic launcher for Spring Boot application.
 *
 * This class allows external systems (like OSGi wrapper) to:
 * - Start the Spring Boot application with custom configuration
 * - Inject external DataSource
 * - Control lifecycle (start/stop)
 * - Configure server port and other properties
 *
 * The application runs completely independently with its own embedded Tomcat server.
 */
public class ApplicationLauncher {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationLauncher.class);

    private ConfigurableApplicationContext context;
    private DataSource externalDataSource;
    private int serverPort = 8080;
    private String contextPath = "/";
    private String[] activeProfiles = {};
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * Set an external DataSource to be used by the application.
     * When set, this DataSource will be registered as a primary bean,
     * overriding any auto-configured DataSource.
     */
    public ApplicationLauncher withDataSource(DataSource dataSource) {
        this.externalDataSource = dataSource;
        return this;
    }

    /**
     * Configure the server port for embedded Tomcat.
     */
    public ApplicationLauncher withPort(int port) {
        this.serverPort = port;
        return this;
    }

    /**
     * Configure the application context path.
     */
    public ApplicationLauncher withContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    /**
     * Set active Spring profiles.
     */
    public ApplicationLauncher withProfiles(String... profiles) {
        this.activeProfiles = profiles;
        return this;
    }

    /**
     * Add custom properties.
     */
    public ApplicationLauncher withProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
        return this;
    }

    /**
     * Start the Spring Boot application.
     *
     * @return true if started successfully, false otherwise
     */
    public boolean start() {
        if (context != null && context.isActive()) {
            logger.warn("Application is already running");
            return false;
        }

        try {
            logger.info("Starting Spring Boot application...");
            logger.info("Server port: {}", serverPort);
            logger.info("Context path: {}", contextPath);
            logger.info("External DataSource provided: {}", externalDataSource != null);

            SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringBootJdbcApplication.class)
                    .bannerMode(Banner.Mode.LOG);

            // Configure properties
            Map<String, Object> properties = new HashMap<>(additionalProperties);
            properties.put("server.port", serverPort);
            properties.put("server.servlet.context-path", contextPath);

            // If external DataSource is provided, disable auto-configuration
            if (externalDataSource != null) {
                properties.put("spring.datasource.type", "com.zaxxer.hikari.HikariDataSource");
                // Register the external DataSource as a bean
                builder.initializers(applicationContext -> {
                    applicationContext.getBeanFactory()
                            .registerSingleton("externalDataSource", externalDataSource);
                });
                // Tell the app to use external DataSource
                properties.put("app.datasource.external", true);
            }

            builder.properties(properties);

            if (activeProfiles.length > 0) {
                builder.profiles(activeProfiles);
            }

            context = builder.run();
            logger.info("Spring Boot application started successfully");
            return true;

        } catch (Exception e) {
            logger.error("Failed to start Spring Boot application", e);
            return false;
        }
    }

    /**
     * Stop the Spring Boot application gracefully.
     *
     * @return true if stopped successfully, false otherwise
     */
    public boolean stop() {
        if (context == null || !context.isActive()) {
            logger.warn("Application is not running");
            return false;
        }

        try {
            logger.info("Stopping Spring Boot application...");
            context.close();
            context = null;
            logger.info("Spring Boot application stopped successfully");
            return true;

        } catch (Exception e) {
            logger.error("Failed to stop Spring Boot application", e);
            return false;
        }
    }

    /**
     * Check if the application is running.
     */
    public boolean isRunning() {
        return context != null && context.isActive();
    }

    /**
     * Get the Spring application context.
     */
    public ConfigurableApplicationContext getContext() {
        return context;
    }

    /**
     * Get the actual server port (useful when configured with port 0 for random port).
     */
    public int getActualPort() {
        if (context != null && context.isActive()) {
            try {
                return context.getEnvironment()
                        .getProperty("local.server.port", Integer.class, serverPort);
            } catch (Exception e) {
                logger.debug("Could not determine actual port", e);
            }
        }
        return serverPort;
    }
}
