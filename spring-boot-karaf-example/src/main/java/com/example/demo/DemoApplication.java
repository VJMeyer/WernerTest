package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Spring Boot Application that can run:
 * 1. Standalone with embedded Tomcat
 * 2. In external servlet container (Karaf PAX Web)
 * 3. As Docker container
 *
 * Extends SpringBootServletInitializer for WAR deployment support.
 */
@SpringBootApplication
public class DemoApplication extends SpringBootServletInitializer {

    /**
     * Required for WAR deployment in external servlet containers like Karaf's PAX Web.
     * This method configures the application when deployed as a WAR file.
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DemoApplication.class);
    }

    /**
     * Main method for standalone execution with embedded Tomcat.
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
