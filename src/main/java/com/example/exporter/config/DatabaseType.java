package com.example.exporter.config;

/**
 * Supported database types for the exporter.
 *
 * Each database has specific requirements for streaming large result sets:
 * - PostgreSQL: Requires autoCommit=false and setFetchSize() for server-side cursors
 * - Oracle: Uses setFetchSize() for prefetch, works with autoCommit=true
 * - MSSQL: Uses adaptive buffering and cursor types for streaming
 */
public enum DatabaseType {
    /**
     * PostgreSQL database.
     * Uses server-side cursors with autoCommit=false.
     */
    POSTGRESQL("postgresql"),

    /**
     * Oracle database.
     * Uses prefetch rows with Oracle-specific optimizations.
     */
    ORACLE("oracle"),

    /**
     * Microsoft SQL Server.
     * Uses adaptive buffering and forward-only cursors.
     */
    MSSQL("sqlserver");

    private final String urlIdentifier;

    DatabaseType(String urlIdentifier) {
        this.urlIdentifier = urlIdentifier;
    }

    /**
     * Get the identifier used in JDBC URLs.
     */
    public String getUrlIdentifier() {
        return urlIdentifier;
    }

    /**
     * Detect database type from JDBC URL.
     *
     * @param jdbcUrl The JDBC connection URL
     * @return The detected database type
     * @throws IllegalArgumentException if database type cannot be determined
     */
    public static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("JDBC URL cannot be null or empty");
        }

        String lowerUrl = jdbcUrl.toLowerCase();

        if (lowerUrl.contains(":postgresql:") || lowerUrl.contains(":pgsql:")) {
            return POSTGRESQL;
        } else if (lowerUrl.contains(":oracle:") || lowerUrl.contains(":thin:")) {
            return ORACLE;
        } else if (lowerUrl.contains(":sqlserver:") || lowerUrl.contains(":microsoft:")) {
            return MSSQL;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported database type in URL: " + jdbcUrl +
                            ". Supported: PostgreSQL, Oracle, MSSQL"
            );
        }
    }
}
