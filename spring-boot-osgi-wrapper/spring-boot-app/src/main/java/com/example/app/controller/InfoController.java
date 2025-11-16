package com.example.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    @Value("${spring.application.name:Spring Boot JDBC App}")
    private String appName;

    @Value("${app.datasource.external:false}")
    private boolean externalDataSource;

    @Value("${server.port:8080}")
    private int serverPort;

    @GetMapping
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", appName);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("serverPort", serverPort);
        info.put("externalDataSource", externalDataSource);

        // Detect if running in OSGi
        boolean inOsgi = detectOsgiEnvironment();
        info.put("osgiEnvironment", inOsgi);

        if (inOsgi) {
            info.put("runtime", "Managed by OSGi/Karaf");
        } else {
            info.put("runtime", "Standalone Spring Boot");
        }

        return info;
    }

    private boolean detectOsgiEnvironment() {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
