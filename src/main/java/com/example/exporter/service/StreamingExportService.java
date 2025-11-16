package com.example.exporter.service;

import com.example.exporter.cache.LookupCache;
import com.example.exporter.config.DatabaseStreamingConfig;
import com.example.exporter.config.ExporterProperties;
import com.example.exporter.model.ExportDefinition;
import com.example.exporter.writer.StreamingWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core service for streaming large datasets from PostgreSQL, Oracle, and MSSQL.
 *
 * Key features:
 * - Uses database-specific cursors for memory-efficient streaming
 * - Applies in-memory lookup resolution to avoid expensive JOINs
 * - Streams data directly to output without buffering
 * - Supports configurable fetch size for optimal performance
 * - Auto-detects database type and applies optimal configurations
 *
 * Performance characteristics:
 * - Memory usage: O(fetchSize) instead of O(totalRows)
 * - Network: Efficient streaming with configurable flush intervals
 * - CPU: Lookup resolution in O(1) time using cached maps
 */
@Service
public class StreamingExportService {

    private static final Logger log = LoggerFactory.getLogger(StreamingExportService.class);

    private final DataSource dataSource;
    private final LookupCache lookupCache;
    private final ExporterProperties properties;
    private final DatabaseStreamingConfig dbConfig;

    public StreamingExportService(DataSource dataSource,
                                  LookupCache lookupCache,
                                  ExporterProperties properties,
                                  DatabaseStreamingConfig dbConfig) {
        this.dataSource = dataSource;
        this.lookupCache = lookupCache;
        this.properties = properties;
        this.dbConfig = dbConfig;
    }

    /**
     * Export data using a streaming writer.
     *
     * @param exportDefinition The export configuration
     * @param writer The streaming writer (JSON or CSV)
     * @param outputStream The output stream to write to
     * @return The number of rows exported
     * @throws SQLException If a database error occurs
     * @throws IOException If an I/O error occurs
     */
    public long export(ExportDefinition exportDefinition,
                       StreamingWriter writer,
                       OutputStream outputStream) throws SQLException, IOException {

        log.info("Starting export '{}' with query: {} (Database: {})",
                exportDefinition.name(), exportDefinition.mainQuery(), dbConfig.getDatabaseType());

        List<String> outputColumns = exportDefinition.getAllOutputColumns();
        writer.start(outputStream, outputColumns);

        long rowCount = 0;
        int flushInterval = properties.getStreaming().getFlushInterval();
        int fetchSize = dbConfig.getOptimalFetchSize();

        try (Connection connection = dataSource.getConnection()) {
            // Apply database-specific optimizations
            dbConfig.optimizeConnection(connection);

            try (PreparedStatement stmt = connection.prepareStatement(
                    exportDefinition.mainQuery(),
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY)) {

                // Set fetch size - controls how many rows are fetched at a time
                // This is the key to memory-efficient streaming
                stmt.setFetchSize(fetchSize);

                log.info("Executing query with fetch size: {} ({})", fetchSize, dbConfig.getDatabaseType());

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();

                    while (rs.next()) {
                        Map<String, Object> row = processRow(rs, metaData, exportDefinition);
                        writer.writeRow(row);
                        rowCount++;

                        // Periodic flush to prevent buffering
                        if (rowCount % flushInterval == 0) {
                            writer.flush();
                            outputStream.flush();

                            if (rowCount % 1000000 == 0) {
                                log.info("Exported {} rows...", rowCount);
                            }
                        }
                    }
                }
            }

            // Database-specific cleanup
            dbConfig.cleanupConnection(connection);
        }

        writer.end();
        outputStream.flush();

        log.info("Export '{}' completed: {} total rows", exportDefinition.name(), rowCount);
        return rowCount;
    }

    /**
     * Process a single row from the ResultSet.
     *
     * This method:
     * 1. Extracts column values from the ResultSet
     * 2. Resolves lookup values for foreign keys
     * 3. Returns a complete row with all output columns
     */
    private Map<String, Object> processRow(ResultSet rs,
                                           ResultSetMetaData metaData,
                                           ExportDefinition exportDefinition) throws SQLException {
        // Use LinkedHashMap to preserve column order
        Map<String, Object> row = new LinkedHashMap<>();

        // Extract values from ResultSet
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = rs.getObject(i);
            row.put(columnName, value);
        }

        // Apply lookup resolutions
        for (Map.Entry<String, ExportDefinition.LookupResolution> entry :
                exportDefinition.lookupResolutions().entrySet()) {

            String fkColumn = entry.getKey();
            ExportDefinition.LookupResolution resolution = entry.getValue();

            Object fkValue = row.get(fkColumn);

            if (fkValue != null && lookupCache.hasLookup(resolution.lookupName())) {
                Map<String, Object> lookupValues = lookupCache.get(resolution.lookupName(), fkValue);

                // Add all resolved lookup columns to the row
                for (String outputColumn : resolution.outputColumns()) {
                    Object lookupValue = lookupValues.get(outputColumn);
                    row.put(outputColumn, lookupValue);
                }
            } else {
                // FK is null or lookup not found, set lookup columns to null
                for (String outputColumn : resolution.outputColumns()) {
                    row.put(outputColumn, null);
                }
            }
        }

        return row;
    }

    /**
     * Export with a simple query (no lookup resolution).
     *
     * @param query The SQL query to execute
     * @param writer The streaming writer
     * @param outputStream The output stream
     * @return The number of rows exported
     */
    public long exportSimple(String query,
                             StreamingWriter writer,
                             OutputStream outputStream) throws SQLException, IOException {

        log.info("Starting simple export with query: {} (Database: {})", query, dbConfig.getDatabaseType());

        long rowCount = 0;
        int flushInterval = properties.getStreaming().getFlushInterval();
        int fetchSize = dbConfig.getOptimalFetchSize();

        try (Connection connection = dataSource.getConnection()) {
            // Apply database-specific optimizations
            dbConfig.optimizeConnection(connection);

            try (PreparedStatement stmt = connection.prepareStatement(
                    query,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY)) {

                stmt.setFetchSize(fetchSize);

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();

                    // Initialize writer with column names
                    List<String> columns = new java.util.ArrayList<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        columns.add(metaData.getColumnLabel(i));
                    }
                    writer.start(outputStream, columns);

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            row.put(metaData.getColumnLabel(i), rs.getObject(i));
                        }
                        writer.writeRow(row);
                        rowCount++;

                        if (rowCount % flushInterval == 0) {
                            writer.flush();
                            outputStream.flush();

                            if (rowCount % 1000000 == 0) {
                                log.info("Exported {} rows...", rowCount);
                            }
                        }
                    }
                }
            }

            // Database-specific cleanup
            dbConfig.cleanupConnection(connection);
        }

        writer.end();
        outputStream.flush();

        log.info("Simple export completed: {} total rows", rowCount);
        return rowCount;
    }
}
