package com.example.exporter.model;

import java.util.Map;

/**
 * Defines a lookup table configuration.
 *
 * @param tableName The database table name
 * @param keyColumn The column to use as the cache key (usually the foreign key)
 * @param valueColumns Map of column name to output field name
 * @param query Optional custom query (if null, auto-generated from table/columns)
 */
public record LookupDefinition(
        String tableName,
        String keyColumn,
        Map<String, String> valueColumns,
        String query
) {
    public LookupDefinition(String tableName, String keyColumn, Map<String, String> valueColumns) {
        this(tableName, keyColumn, valueColumns, null);
    }

    /**
     * Generate the SQL query for this lookup definition.
     */
    public String generateQuery() {
        if (query != null && !query.isBlank()) {
            return query;
        }

        StringBuilder columns = new StringBuilder(keyColumn);
        for (String column : valueColumns.keySet()) {
            columns.append(", ").append(column);
        }

        return String.format("SELECT %s FROM %s", columns, tableName);
    }
}
