package com.example.exporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * High-Speed PostgreSQL Data Exporter
 *
 * This application provides streaming export capabilities for large PostgreSQL tables
 * (30M - 300M records) with support for JSON and CSV output formats.
 *
 * Key features:
 * - Streaming output to prevent memory issues
 * - In-memory caching of lookup tables
 * - Virtual threads for high concurrency (Java 21)
 * - Content negotiation for JSON/CSV based on Accept header
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
