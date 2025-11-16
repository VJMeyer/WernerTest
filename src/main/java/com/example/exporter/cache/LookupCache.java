package com.example.exporter.cache;

import com.example.exporter.config.ExporterProperties;
import com.example.exporter.model.LookupDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache for lookup tables.
 *
 * This cache stores small reference tables (10-500 entries) in memory
 * to avoid expensive JOINs when exporting large datasets.
 *
 * The cache is designed for high-throughput read operations with periodic refresh.
 */
@Component
public class LookupCache {

    private static final Logger log = LoggerFactory.getLogger(LookupCache.class);

    private final JdbcTemplate jdbcTemplate;
    private final ExporterProperties properties;
    private final ScheduledExecutorService scheduler;

    // Cache structure: lookupName -> (key -> columnValues)
    private final Map<String, Map<Object, Map<String, Object>>> caches = new ConcurrentHashMap<>();

    // Lookup definitions
    private final Map<String, LookupDefinition> definitions = new ConcurrentHashMap<>();

    public LookupCache(JdbcTemplate jdbcTemplate, ExporterProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lookup-cache-refresh");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic cache refresh
        scheduler.scheduleWithFixedDelay(
                this::refreshAllCaches,
                properties.getCache().getTtlSeconds(),
                properties.getCache().getTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    /**
     * Register a lookup definition and load it into cache.
     *
     * @param name The unique name for this lookup
     * @param definition The lookup definition
     */
    public void registerLookup(String name, LookupDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Lookup definition cannot be null");
        }

        definitions.put(name, definition);
        loadCache(name, definition);
        log.info("Registered lookup '{}' from table '{}' with {} columns",
                name, definition.tableName(), definition.valueColumns().size());
    }

    /**
     * Get a lookup value by key.
     *
     * @param lookupName The name of the lookup cache
     * @param key The key value (foreign key from main table)
     * @return Map of column name to value, or empty map if not found
     */
    public Map<String, Object> get(String lookupName, Object key) {
        Map<Object, Map<String, Object>> cache = caches.get(lookupName);
        if (cache == null) {
            log.warn("Lookup cache '{}' not found", lookupName);
            return Map.of();
        }

        Map<String, Object> result = cache.get(key);
        return result != null ? result : Map.of();
    }

    /**
     * Check if a lookup is registered.
     */
    public boolean hasLookup(String lookupName) {
        return caches.containsKey(lookupName);
    }

    /**
     * Get the size of a specific cache.
     */
    public int getCacheSize(String lookupName) {
        Map<Object, Map<String, Object>> cache = caches.get(lookupName);
        return cache != null ? cache.size() : 0;
    }

    /**
     * Manually refresh a specific cache.
     */
    public void refreshCache(String lookupName) {
        LookupDefinition definition = definitions.get(lookupName);
        if (definition != null) {
            loadCache(lookupName, definition);
            log.info("Refreshed lookup cache '{}'", lookupName);
        }
    }

    /**
     * Refresh all caches.
     */
    public void refreshAllCaches() {
        log.debug("Refreshing all lookup caches");
        definitions.forEach(this::loadCache);
    }

    private void loadCache(String name, LookupDefinition definition) {
        try {
            Map<Object, Map<String, Object>> newCache = new ConcurrentHashMap<>();
            String sql = definition.generateQuery();

            log.debug("Loading cache '{}' with query: {}", name, sql);

            jdbcTemplate.query(sql, rs -> {
                Object key = rs.getObject(definition.keyColumn());
                Map<String, Object> values = new ConcurrentHashMap<>();

                for (Map.Entry<String, String> entry : definition.valueColumns().entrySet()) {
                    String dbColumn = entry.getKey();
                    String outputName = entry.getValue();
                    values.put(outputName, rs.getObject(dbColumn));
                }

                newCache.put(key, values);
            });

            // Check size constraint
            if (newCache.size() > properties.getCache().getMaxSize()) {
                log.warn("Lookup '{}' has {} entries, exceeding max size {}. Consider a different approach.",
                        name, newCache.size(), properties.getCache().getMaxSize());
            }

            // Atomically replace the cache
            caches.put(name, newCache);

            log.info("Loaded {} entries into cache '{}'", newCache.size(), name);
        } catch (Exception e) {
            log.error("Failed to load cache '{}': {}", name, e.getMessage(), e);
            // Keep existing cache if refresh fails
            if (!caches.containsKey(name)) {
                caches.put(name, new ConcurrentHashMap<>());
            }
        }
    }

    /**
     * Clear all caches and shutdown the refresh scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
        caches.clear();
        definitions.clear();
        log.info("Lookup cache shutdown complete");
    }
}
