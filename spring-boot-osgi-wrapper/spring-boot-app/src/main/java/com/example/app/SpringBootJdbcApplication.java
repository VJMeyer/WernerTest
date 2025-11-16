package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application.
 *
 * Can be started:
 * 1. Standalone via main() method
 * 2. Programmatically via ApplicationLauncher (for OSGi integration)
 * 3. As Docker container
 */
@SpringBootApplication
public class SpringBootJdbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootJdbcApplication.class, args);
    }
}
