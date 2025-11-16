package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Information controller to display runtime environment details.
 * Useful for verifying which environment the application is running in.
 */
@RestController
@RequestMapping("/api/info")
public class InfoController {

    @Value("${spring.application.name:Spring Boot Karaf JDBC App}")
    private String applicationName;

    @Value("${app.environment:unknown}")
    private String environment;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("application", applicationName);
        info.put("configuredEnvironment", environment);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));

        // Detect runtime environment
        String runtimeEnvironment = detectRuntimeEnvironment();
        info.put("detectedRuntime", runtimeEnvironment);

        // OSGi detection
        boolean isOsgi = isRunningInOsgi();
        info.put("osgiEnvironment", isOsgi);

        if (isOsgi) {
            info.put("osgiFramework", getOsgiFrameworkInfo());
        }

        // Servlet container info
        info.put("servletContainer", getServletContainerInfo());

        return ResponseEntity.ok(info);
    }

    private String detectRuntimeEnvironment() {
        // Check for OSGi
        if (isRunningInOsgi()) {
            return "Apache Karaf OSGi";
        }

        // Check for embedded Tomcat
        try {
            Class.forName("org.apache.catalina.startup.Tomcat");
            return "Embedded Tomcat (Standalone)";
        } catch (ClassNotFoundException e) {
            // Not embedded Tomcat
        }

        // Check if running in external servlet container
        String serverInfo = System.getProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE");
        if (serverInfo != null) {
            return "External Tomcat";
        }

        return "Unknown";
    }

    private boolean isRunningInOsgi() {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");

            // Verify we actually have a bundle context
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");
            Object bundle = frameworkUtilClass.getMethod("getBundle", Class.class)
                    .invoke(null, this.getClass());

            return bundle != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getOsgiFrameworkInfo() {
        try {
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");
            Object bundle = frameworkUtilClass.getMethod("getBundle", Class.class)
                    .invoke(null, this.getClass());

            if (bundle != null) {
                Object bundleContext = bundle.getClass().getMethod("getBundleContext").invoke(bundle);
                if (bundleContext != null) {
                    String frameworkVendor = (String) bundleContext.getClass()
                            .getMethod("getProperty", String.class)
                            .invoke(bundleContext, "org.osgi.framework.vendor");

                    String frameworkVersion = (String) bundleContext.getClass()
                            .getMethod("getProperty", String.class)
                            .invoke(bundleContext, "org.osgi.framework.version");

                    return frameworkVendor + " " + frameworkVersion;
                }
            }
        } catch (Exception e) {
            return "Unknown OSGi Framework";
        }
        return "OSGi Framework";
    }

    private String getServletContainerInfo() {
        try {
            // Try to get Tomcat version
            Class<?> serverInfoClass = Class.forName("org.apache.catalina.util.ServerInfo");
            String version = (String) serverInfoClass.getMethod("getServerInfo").invoke(null);
            return version;
        } catch (Exception e) {
            // Not Tomcat or can't determine
        }

        try {
            // Try Jetty (used by Karaf's PAX Web)
            Class<?> serverClass = Class.forName("org.eclipse.jetty.server.Server");
            String version = (String) serverClass.getMethod("getVersion").invoke(null);
            return "Jetty " + version;
        } catch (Exception e) {
            // Not Jetty
        }

        return "Unknown Servlet Container";
    }
}
