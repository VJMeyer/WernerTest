package com.example.osgi;

import com.example.app.ApplicationLauncher;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Alternative lifecycle manager using BundleActivator.
 *
 * This is a simpler approach than Declarative Services, but requires
 * manual service tracking for DataSource.
 *
 * To use this instead of SpringBootLifecycleManager:
 * 1. Add Bundle-Activator header to MANIFEST.MF
 * 2. Disable DS annotations in maven-bundle-plugin
 *
 * This version demonstrates manual OSGi service tracking.
 */
public class SpringBootBundleActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootBundleActivator.class);

    private ApplicationLauncher launcher;
    private ServiceTracker<DataSource, DataSource> dataSourceTracker;
    private BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("=== Bundle STARTING ===");
        this.bundleContext = context;

        // Create service tracker for DataSource
        Filter filter = context.createFilter(
                "(&(objectClass=javax.sql.DataSource)(dataSourceName=AppDataSource))");

        dataSourceTracker = new ServiceTracker<>(context, filter, new ServiceTrackerCustomizer<>() {

            @Override
            public DataSource addingService(ServiceReference<DataSource> reference) {
                DataSource dataSource = context.getService(reference);
                logger.info("DataSource service found: {}", dataSource.getClass().getName());

                // Start Spring Boot application with the DataSource
                startSpringBoot(dataSource);

                return dataSource;
            }

            @Override
            public void modifiedService(ServiceReference<DataSource> reference, DataSource service) {
                logger.info("DataSource service modified");
            }

            @Override
            public void removedService(ServiceReference<DataSource> reference, DataSource service) {
                logger.info("DataSource service removed");
                stopSpringBoot();
                context.ungetService(reference);
            }
        });

        dataSourceTracker.open();
        logger.info("DataSource service tracker opened");

        // Check if DataSource is already available
        DataSource ds = dataSourceTracker.getService();
        if (ds == null) {
            logger.warn("DataSource not yet available, waiting for service registration...");
            logger.info("Configure DataSource in Karaf: " +
                    "copy datasource config to $KARAF_HOME/etc/org.ops4j.datasource-AppDataSource.cfg");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("=== Bundle STOPPING ===");

        stopSpringBoot();

        if (dataSourceTracker != null) {
            dataSourceTracker.close();
            dataSourceTracker = null;
        }

        logger.info("Bundle stopped");
    }

    private void startSpringBoot(DataSource dataSource) {
        if (launcher != null && launcher.isRunning()) {
            logger.warn("Spring Boot application already running");
            return;
        }

        // Read port from bundle properties or system properties
        int port = Integer.getInteger("springboot.server.port", 8090);
        String contextPath = System.getProperty("springboot.context.path", "/");

        logger.info("Starting Spring Boot application on port {}", port);

        launcher = new ApplicationLauncher()
                .withDataSource(dataSource)
                .withPort(port)
                .withContextPath(contextPath)
                .withProfiles("osgi");

        // Start in separate thread
        Thread startThread = new Thread(() -> {
            boolean started = launcher.start();
            if (started) {
                logger.info("Spring Boot application started successfully");
                logger.info("Access: http://localhost:{}{}/api/products", port, contextPath);

                // Register Spring Boot app as OSGi service (optional)
                registerSpringBootService();
            } else {
                logger.error("Failed to start Spring Boot application");
            }
        }, "SpringBoot-Activator-Starter");

        startThread.start();
    }

    private void stopSpringBoot() {
        if (launcher != null && launcher.isRunning()) {
            logger.info("Stopping Spring Boot application");
            launcher.stop();
            launcher = null;
        }
    }

    private void registerSpringBootService() {
        // Optionally register the launcher as an OSGi service
        // This allows other bundles to interact with the Spring Boot app
        if (bundleContext != null && launcher != null) {
            bundleContext.registerService(
                    ApplicationLauncher.class,
                    launcher,
                    null);
            logger.info("ApplicationLauncher registered as OSGi service");
        }
    }
}
