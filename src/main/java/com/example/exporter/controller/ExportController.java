package com.example.exporter.controller;

import com.example.exporter.cache.LookupCache;
import com.example.exporter.model.ExportDefinition;
import com.example.exporter.model.LookupDefinition;
import com.example.exporter.service.StreamingExportService;
import com.example.exporter.writer.CsvStreamingWriter;
import com.example.exporter.writer.JsonStreamingWriter;
import com.example.exporter.writer.ParquetStreamingWriter;
import com.example.exporter.writer.StreamingWriter;
import com.fasterxml.jackson.core.JsonFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for data export operations.
 *
 * Supports streaming export in JSON, CSV, and Parquet formats based on the Accept header.
 * Uses Spring's StreamingResponseBody for true streaming without buffering.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final StreamingExportService exportService;
    private final LookupCache lookupCache;
    private final JsonFactory jsonFactory;

    // Registry of export definitions
    private final Map<String, ExportDefinition> exportDefinitions = new ConcurrentHashMap<>();

    public ExportController(StreamingExportService exportService,
                            LookupCache lookupCache,
                            JsonFactory jsonFactory) {
        this.exportService = exportService;
        this.lookupCache = lookupCache;
        this.jsonFactory = jsonFactory;
    }

    /**
     * Initialize example export definitions.
     *
     * In a real application, these would be loaded from configuration
     * or defined programmatically based on your schema.
     */
    @PostConstruct
    public void initializeExampleDefinitions() {
        // Example: Products export with category and supplier lookups
        registerProductsExport();

        // Example: Orders export with customer and status lookups
        registerOrdersExport();

        log.info("Initialized {} export definitions", exportDefinitions.size());
    }

    private void registerProductsExport() {
        // Register category lookup
        lookupCache.registerLookup("categories", new LookupDefinition(
                "categories",
                "id",
                Map.of(
                        "name", "category_name",
                        "description", "category_description"
                )
        ));

        // Register supplier lookup
        lookupCache.registerLookup("suppliers", new LookupDefinition(
                "suppliers",
                "id",
                Map.of(
                        "name", "supplier_name",
                        "country", "supplier_country"
                )
        ));

        // Define the products export
        ExportDefinition productsExport = new ExportDefinition(
                "products",
                "SELECT id, name, price, category_id, supplier_id, created_at FROM products ORDER BY id",
                List.of("id", "name", "price", "category_id", "supplier_id", "created_at"),
                Map.of(
                        "category_id", new ExportDefinition.LookupResolution(
                                "categories",
                                List.of("category_name", "category_description")
                        ),
                        "supplier_id", new ExportDefinition.LookupResolution(
                                "suppliers",
                                List.of("supplier_name", "supplier_country")
                        )
                )
        );

        exportDefinitions.put("products", productsExport);
    }

    private void registerOrdersExport() {
        // Register customer lookup
        lookupCache.registerLookup("customers", new LookupDefinition(
                "customers",
                "id",
                Map.of(
                        "name", "customer_name",
                        "email", "customer_email"
                )
        ));

        // Register order status lookup
        lookupCache.registerLookup("order_statuses", new LookupDefinition(
                "order_statuses",
                "id",
                Map.of("status_name", "status_name")
        ));

        // Define the orders export
        ExportDefinition ordersExport = new ExportDefinition(
                "orders",
                "SELECT id, customer_id, status_id, total_amount, order_date FROM orders ORDER BY id",
                List.of("id", "customer_id", "status_id", "total_amount", "order_date"),
                Map.of(
                        "customer_id", new ExportDefinition.LookupResolution(
                                "customers",
                                List.of("customer_name", "customer_email")
                        ),
                        "status_id", new ExportDefinition.LookupResolution(
                                "order_statuses",
                                List.of("status_name")
                        )
                )
        );

        exportDefinitions.put("orders", ordersExport);
    }

    /**
     * Export data with automatic format selection based on Accept header.
     *
     * @param name The name of the export definition
     * @param acceptHeader The Accept header for content negotiation
     * @return Streaming response body
     */
    @GetMapping("/{name}")
    public ResponseEntity<StreamingResponseBody> exportData(
            @PathVariable String name,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {

        ExportDefinition definition = exportDefinitions.get(name);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }

        // Determine output format from Accept header
        StreamingWriter writer;
        String fileExtension;
        String formatName;

        if (acceptHeader.contains("application/vnd.apache.parquet") ||
                acceptHeader.contains("application/parquet")) {
            writer = new ParquetStreamingWriter();
            fileExtension = ".parquet";
            formatName = "Parquet";
        } else if (acceptHeader.contains("text/csv") ||
                acceptHeader.contains("application/csv")) {
            writer = new CsvStreamingWriter();
            fileExtension = ".csv";
            formatName = "CSV";
        } else {
            writer = new JsonStreamingWriter(jsonFactory);
            fileExtension = ".json";
            formatName = "JSON";
        }

        String contentType = writer.getContentType();

        log.info("Starting export '{}' in {} format", name, formatName);

        StreamingResponseBody responseBody = outputStream -> {
            try {
                long rowCount = exportService.export(definition, writer, outputStream);
                log.info("Export '{}' completed with {} rows", name, rowCount);
            } catch (Exception e) {
                log.error("Export '{}' failed: {}", name, e.getMessage(), e);
                throw new RuntimeException("Export failed: " + e.getMessage(), e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + fileExtension + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                // Disable response buffering
                .header("X-Accel-Buffering", "no")
                .body(responseBody);
    }

    /**
     * Export data with a custom SQL query.
     *
     * WARNING: This endpoint should be secured in production!
     * Consider using a whitelist of allowed queries or parameterized templates.
     *
     * @param query The SQL query to execute
     * @param acceptHeader The Accept header for content negotiation
     * @return Streaming response body
     */
    @PostMapping("/custom")
    public ResponseEntity<StreamingResponseBody> exportCustomQuery(
            @RequestBody String query,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {

        // Basic SQL injection protection - in production, use a proper whitelist
        if (containsDangerousKeywords(query)) {
            return ResponseEntity.badRequest().build();
        }

        StreamingWriter writer;
        String fileExtension;
        String formatName;

        if (acceptHeader.contains("application/vnd.apache.parquet") ||
                acceptHeader.contains("application/parquet")) {
            writer = new ParquetStreamingWriter();
            fileExtension = ".parquet";
            formatName = "Parquet";
        } else if (acceptHeader.contains("text/csv") ||
                acceptHeader.contains("application/csv")) {
            writer = new CsvStreamingWriter();
            fileExtension = ".csv";
            formatName = "CSV";
        } else {
            writer = new JsonStreamingWriter(jsonFactory);
            fileExtension = ".json";
            formatName = "JSON";
        }

        String contentType = writer.getContentType();

        log.info("Starting custom export in {} format", formatName);

        StreamingResponseBody responseBody = outputStream -> {
            try {
                long rowCount = exportService.exportSimple(query, writer, outputStream);
                log.info("Custom export completed with {} rows", rowCount);
            } catch (Exception e) {
                log.error("Custom export failed: {}", e.getMessage(), e);
                throw new RuntimeException("Export failed: " + e.getMessage(), e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"export" + fileExtension + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("X-Accel-Buffering", "no")
                .body(responseBody);
    }

    /**
     * Get list of available export definitions.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listExports() {
        Map<String, Object> response = Map.of(
                "exports", exportDefinitions.keySet(),
                "count", exportDefinitions.size()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get cache statistics.
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Integer> cacheSizes = new ConcurrentHashMap<>();
        for (String name : List.of("categories", "suppliers", "customers", "order_statuses")) {
            if (lookupCache.hasLookup(name)) {
                cacheSizes.put(name, lookupCache.getCacheSize(name));
            }
        }
        return ResponseEntity.ok(Map.of("caches", cacheSizes));
    }

    /**
     * Refresh all lookup caches.
     */
    @PostMapping("/cache/refresh")
    public ResponseEntity<String> refreshCaches() {
        lookupCache.refreshAllCaches();
        return ResponseEntity.ok("Caches refreshed successfully");
    }

    private boolean containsDangerousKeywords(String query) {
        String upperQuery = query.toUpperCase();
        List<String> dangerous = List.of(
                "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE",
                "ALTER", "CREATE", "GRANT", "REVOKE", "EXECUTE"
        );
        return dangerous.stream().anyMatch(upperQuery::contains);
    }
}
