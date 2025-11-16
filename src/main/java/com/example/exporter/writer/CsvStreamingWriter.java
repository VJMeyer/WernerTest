package com.example.exporter.writer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Streams CSV data directly to output without buffering.
 *
 * Uses Apache Commons CSV for efficient and RFC 4180 compliant CSV generation.
 * Handles proper escaping of special characters and quotes.
 */
public class CsvStreamingWriter implements StreamingWriter {

    private static final Logger log = LoggerFactory.getLogger(CsvStreamingWriter.class);

    private CSVPrinter csvPrinter;
    private OutputStreamWriter writer;
    private List<String> columns;
    private long rowCount = 0;

    @Override
    public void start(OutputStream outputStream, List<String> columns) throws IOException {
        this.columns = columns;
        this.writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        // Configure CSV format
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(columns.toArray(new String[0]))
                .setRecordSeparator("\n")
                .setQuoteMode(org.apache.commons.csv.QuoteMode.MINIMAL)
                .build();

        this.csvPrinter = new CSVPrinter(writer, format);

        log.debug("Started CSV streaming with {} columns", columns.size());
    }

    @Override
    public void writeRow(Map<String, Object> row) throws IOException {
        List<Object> values = new ArrayList<>(columns.size());

        for (String column : columns) {
            Object value = row.get(column);
            values.add(formatValue(value));
        }

        csvPrinter.printRecord(values);
        rowCount++;

        if (rowCount % 100000 == 0) {
            log.debug("Written {} rows to CSV stream", rowCount);
        }
    }

    private Object formatValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof Timestamp ts) {
            return ts.toInstant().toString();
        } else if (value instanceof LocalDateTime ldt) {
            return ldt.toString();
        } else if (value instanceof LocalDate ld) {
            return ld.toString();
        } else if (value instanceof OffsetDateTime odt) {
            return odt.toString();
        } else if (value instanceof byte[] bytes) {
            // Base64 encode binary data for CSV
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } else {
            return value;
        }
    }

    @Override
    public void flush() throws IOException {
        if (csvPrinter != null) {
            csvPrinter.flush();
        }
    }

    @Override
    public void end() throws IOException {
        if (csvPrinter != null) {
            csvPrinter.flush();
            csvPrinter.close();
        }
        log.info("Completed CSV streaming: {} total rows", rowCount);
    }

    @Override
    public String getContentType() {
        return "text/csv";
    }
}
