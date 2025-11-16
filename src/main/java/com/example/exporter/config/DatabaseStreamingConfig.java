package com.example.exporter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration for database-specific streaming optimizations.
 *
 * Different databases require different approaches to stream large result sets
 * without loading everything into memory.
 */
@Component
public class DatabaseStreamingConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStreamingConfig.class);

    private final DataSource dataSource;
    private final ExporterProperties properties;
    private DatabaseType databaseType;

    public DatabaseStreamingConfig(DataSource dataSource, ExporterProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
        detectDatabaseType();
    }

    private void detectDatabaseType() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            this.databaseType = DatabaseType.fromJdbcUrl(url);
            log.info("Detected database type: {}", databaseType);

            // Log database-specific recommendations
            logRecommendations();
        } catch (SQLException e) {
            log.error("Failed to detect database type: {}", e.getMessage());
            // Default to PostgreSQL behavior
            this.databaseType = DatabaseType.POSTGRESQL;
            log.warn("Defaulting to PostgreSQL streaming configuration");
        }
    }

    private void logRecommendations() {
        switch (databaseType) {
            case POSTGRESQL -> log.info(
                    "PostgreSQL: Using server-side cursors with autoCommit=false and fetchSize={}",
                    properties.getFetchSize()
            );
            case ORACLE -> log.info(
                    "Oracle: Using row prefetch with fetchSize={}. Consider setting oracle.jdbc.defaultRowPrefetch",
                    properties.getFetchSize()
            );
            case MSSQL -> log.info(
                    "MSSQL: Using adaptive buffering with fetchSize={}. Ensure responseBuffering=adaptive in connection URL",
                    properties.getFetchSize()
            );
        }
    }

    /**
     * Get the detected database type.
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * Check if the connection requires autoCommit=false for streaming.
     * PostgreSQL requires this for server-side cursors.
     */
    public boolean requiresAutoCommitFalse() {
        return databaseType == DatabaseType.POSTGRESQL;
    }

    /**
     * Check if connection should be rolled back after query (no changes made).
     * PostgreSQL needs this to properly close the server-side cursor.
     */
    public boolean requiresRollbackAfterQuery() {
        return databaseType == DatabaseType.POSTGRESQL;
    }

    /**
     * Get the optimal fetch size for the database type.
     * Can be overridden per-database if needed.
     */
    public int getOptimalFetchSize() {
        return switch (databaseType) {
            case POSTGRESQL -> properties.getFetchSize();
            case ORACLE -> {
                // Oracle performs well with larger fetch sizes
                int oracleFetchSize = properties.getFetchSize();
                if (oracleFetchSize < 1000) {
                    log.warn("Oracle performs better with fetch sizes >= 1000. Current: {}", oracleFetchSize);
                }
                yield oracleFetchSize;
            }
            case MSSQL -> {
                // MSSQL works best with moderate fetch sizes
                int mssqlFetchSize = Math.min(properties.getFetchSize(), 10000);
                if (mssqlFetchSize != properties.getFetchSize()) {
                    log.info("Adjusted MSSQL fetch size to {} (max recommended: 10000)", mssqlFetchSize);
                }
                yield mssqlFetchSize;
            }
        };
    }

    /**
     * Get database-specific connection initialization SQL.
     * Returns null if no initialization is needed.
     */
    public String getConnectionInitSql() {
        return switch (databaseType) {
            case POSTGRESQL -> "SET statement_timeout = '3600000'"; // 1 hour timeout
            case ORACLE -> "ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS'";
            case MSSQL -> null; // MSSQL doesn't need special init
        };
    }

    /**
     * Apply database-specific optimizations to a connection before querying.
     *
     * @param connection The JDBC connection
     * @throws SQLException If optimization fails
     */
    public void optimizeConnection(Connection connection) throws SQLException {
        switch (databaseType) {
            case POSTGRESQL -> {
                connection.setAutoCommit(false);
                connection.setReadOnly(true);
            }
            case ORACLE -> {
                // Oracle doesn't require autoCommit=false for streaming
                connection.setReadOnly(true);
            }
            case MSSQL -> {
                // MSSQL uses adaptive buffering
                connection.setAutoCommit(false); // Helps with cursor behavior
                connection.setReadOnly(true);
            }
        }
    }

    /**
     * Clean up connection after query execution.
     *
     * @param connection The JDBC connection
     * @throws SQLException If cleanup fails
     */
    public void cleanupConnection(Connection connection) throws SQLException {
        if (requiresRollbackAfterQuery() && !connection.getAutoCommit()) {
            connection.rollback();
        }
    }
}
