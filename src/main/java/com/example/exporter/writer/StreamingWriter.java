package com.example.exporter.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Interface for streaming data writers.
 *
 * Implementations write data directly to an OutputStream without
 * buffering the entire result set in memory.
 */
public interface StreamingWriter {

    /**
     * Initialize the writer and write any header information.
     *
     * @param outputStream The output stream to write to
     * @param columns The column names in order
     * @throws IOException If an I/O error occurs
     */
    void start(OutputStream outputStream, List<String> columns) throws IOException;

    /**
     * Write a single row of data.
     *
     * @param row Map of column name to value
     * @throws IOException If an I/O error occurs
     */
    void writeRow(Map<String, Object> row) throws IOException;

    /**
     * Flush any buffered data to the output stream.
     *
     * @throws IOException If an I/O error occurs
     */
    void flush() throws IOException;

    /**
     * Complete the output and write any footer information.
     *
     * @throws IOException If an I/O error occurs
     */
    void end() throws IOException;

    /**
     * Get the MIME type for this writer's output format.
     *
     * @return The MIME type string
     */
    String getContentType();
}
