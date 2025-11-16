package com.example.osgi;

import com.example.app.ApplicationLauncher;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Dictionary;
import java.util.Map;

/**
 * OSGi Declarative Services Component that manages Spring Boot application lifecycle.
 *
 * This component:
 * 1. Receives activate/deactivate callbacks from OSGi runtime
 * 2. Injects DataSource from OSGi service registry (JNDI)
 * 3. Starts/stops the Spring Boot application programmatically
 *
 * The Spring Boot application runs completely independently with its own embedded Tomcat server.
 * Karaf/OSGi only provides:
 * - Lifecycle management (start/stop)
 * - DataSource provisioning (via JNDI/service registry)
 * - Optional proxy configuration
 */
@Component(
        name = "spring-boot-jdbc-app",
        immediate = true,
        configurationPid = "com.example.springboot",
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
public class SpringBootLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootLifecycleManager.class);

    private ApplicationLauncher launcher;
    private DataSource dataSource;

    // Configurable properties (can be set via Karaf ConfigAdmin)
    private int serverPort = 8090;
    private String contextPath = "/";
    private String[] activeProfiles = {"osgi"};

    /**
     * Reference to DataSource service from OSGi service registry.
     * This is typically provided by Karaf's JNDI/JDBC subsystem.
     *
     * The DataSource is injected before activate() is called.
     * If no DataSource is available, the component won't start (mandatory reference).
     *
     * To make DataSource optional, change cardinality to OPTIONAL and handle null.
     */
    @Reference(
            name = "dataSource",
            service = DataSource.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY,
            target = "(dataSourceName=AppDataSource)"
    )
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        logger.info("DataSource injected from OSGi service registry: {}", dataSource.getClass().getName());
    }

    public void unsetDataSource(DataSource dataSource) {
        logger.info("DataSource removed from OSGi service registry");
        this.dataSource = null;
    }

    /**
     * OSGi activate lifecycle callback.
     * Called when the bundle is started and all mandatory references are satisfied.
     *
     * This method starts the Spring Boot application with:
     * - The injected DataSource from OSGi
     * - Configured server port
     * - Configured context path
     * - Active Spring profiles
     */
    @Activate
    public void activate(ComponentContext context, Map<String, Object> config) {
        logger.info("=== OSGi Lifecycle: ACTIVATING Spring Boot Application ===");

        // Read configuration from ConfigAdmin (if provided)
        if (config != null && !config.isEmpty()) {
            serverPort = getConfigValue(config, "server.port", serverPort);
            contextPath = getConfigValue(config, "server.context-path", contextPath);
            String profiles = getConfigValue(config, "spring.profiles.active", String.join(",", activeProfiles));
            activeProfiles = profiles.split(",");
        }

        // Also check component properties
        Dictionary<String, Object> properties = context.getProperties();
        if (properties != null) {
            Object portProp = properties.get("server.port");
            if (portProp != null) {
                serverPort = Integer.parseInt(portProp.toString());
            }
        }

        logger.info("Configuration:");
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Context Path: {}", contextPath);
        logger.info("  Active Profiles: {}", String.join(", ", activeProfiles));
        logger.info("  DataSource Available: {}", dataSource != null);

        // Create and configure the launcher
        launcher = new ApplicationLauncher()
                .withDataSource(dataSource)
                .withPort(serverPort)
                .withContextPath(contextPath)
                .withProfiles(activeProfiles)
                .withProperty("spring.main.banner-mode", "off")
                .withProperty("app.osgi.managed", true);

        // Start Spring Boot application in a separate thread
        // This prevents blocking the OSGi activation thread
        Thread startThread = new Thread(() -> {
            try {
                boolean started = launcher.start();
                if (started) {
                    logger.info("=== Spring Boot Application STARTED ===");
                    logger.info("  Actual Port: {}", launcher.getActualPort());
                    logger.info("  Access URL: http://localhost:{}{}/api/products", launcher.getActualPort(), contextPath);
                } else {
                    logger.error("Failed to start Spring Boot application");
                }
            } catch (Exception e) {
                logger.error("Exception while starting Spring Boot application", e);
            }
        }, "SpringBoot-Starter");

        startThread.setDaemon(false);
        startThread.start();

        logger.info("Spring Boot application startup initiated");
    }

    /**
     * OSGi deactivate lifecycle callback.
     * Called when the bundle is stopped or uninstalled.
     *
     * This method gracefully shuts down the Spring Boot application.
     */
    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.info("=== OSGi Lifecycle: DEACTIVATING Spring Boot Application ===");

        if (launcher != null && launcher.isRunning()) {
            boolean stopped = launcher.stop();
            if (stopped) {
                logger.info("=== Spring Boot Application STOPPED ===");
            } else {
                logger.warn("Spring Boot application may not have stopped cleanly");
            }
        } else {
            logger.info("Spring Boot application was not running");
        }

        launcher = null;
        logger.info("OSGi component deactivated");
    }

    /**
     * Optional: Handle configuration updates via ConfigAdmin.
     * This allows runtime reconfiguration without restart.
     */
    @Modified
    public void modified(Map<String, Object> config) {
        logger.info("Configuration updated via ConfigAdmin");

        if (config != null) {
            int newPort = getConfigValue(config, "server.port", serverPort);
            String newContextPath = getConfigValue(config, "server.context-path", contextPath);

            // If port or context path changed, restart is required
            if (newPort != serverPort || !newContextPath.equals(contextPath)) {
                logger.info("Server configuration changed, restart required");
                logger.info("  Old: port={}, contextPath={}", serverPort, contextPath);
                logger.info("  New: port={}, contextPath={}", newPort, newContextPath);

                // Store new values
                serverPort = newPort;
                contextPath = newContextPath;

                // Note: Full restart would require stopping and restarting the application
                // This is a simplified version that just logs the change
                logger.warn("To apply new configuration, restart the bundle: bundle:restart <id>");
            }
        }
    }

    /**
     * Check if Spring Boot application is running.
     */
    public boolean isRunning() {
        return launcher != null && launcher.isRunning();
    }

    /**
     * Get the actual server port.
     */
    public int getActualPort() {
        return launcher != null ? launcher.getActualPort() : serverPort;
    }

    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(Map<String, Object> config, String key, T defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(value.toString());
            } else if (defaultValue instanceof String) {
                return (T) value.toString();
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(value.toString());
            }
        } catch (Exception e) {
            logger.warn("Failed to parse config value for {}: {}", key, e.getMessage());
        }
        return defaultValue;
    }
}
