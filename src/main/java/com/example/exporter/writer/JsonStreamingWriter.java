package com.example.exporter.writer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Streams JSON data directly to output without buffering.
 *
 * Uses Jackson's streaming API (JsonGenerator) for memory-efficient
 * JSON generation. Outputs a JSON array of objects.
 *
 * Example output:
 * [
 *   {"id": 1, "name": "Product A", "category_name": "Electronics"},
 *   {"id": 2, "name": "Product B", "category_name": "Clothing"}
 * ]
 */
public class JsonStreamingWriter implements StreamingWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonStreamingWriter.class);

    private final JsonFactory jsonFactory;
    private JsonGenerator generator;
    private List<String> columns;
    private long rowCount = 0;

    public JsonStreamingWriter(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public void start(OutputStream outputStream, List<String> columns) throws IOException {
        this.columns = columns;
        this.generator = jsonFactory.createGenerator(outputStream);

        // Configure generator for streaming
        generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        generator.configure(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, true);

        // Start the JSON array
        generator.writeStartArray();

        log.debug("Started JSON streaming with {} columns", columns.size());
    }

    @Override
    public void writeRow(Map<String, Object> row) throws IOException {
        generator.writeStartObject();

        for (String column : columns) {
            Object value = row.get(column);
            generator.writeFieldName(column);
            writeValue(value);
        }

        generator.writeEndObject();
        rowCount++;

        if (rowCount % 100000 == 0) {
            log.debug("Written {} rows to JSON stream", rowCount);
        }
    }

    private void writeValue(Object value) throws IOException {
        if (value == null) {
            generator.writeNull();
        } else if (value instanceof String s) {
            generator.writeString(s);
        } else if (value instanceof Integer i) {
            generator.writeNumber(i);
        } else if (value instanceof Long l) {
            generator.writeNumber(l);
        } else if (value instanceof Double d) {
            generator.writeNumber(d);
        } else if (value instanceof Float f) {
            generator.writeNumber(f);
        } else if (value instanceof BigDecimal bd) {
            generator.writeNumber(bd);
        } else if (value instanceof Boolean b) {
            generator.writeBoolean(b);
        } else if (value instanceof Timestamp ts) {
            generator.writeString(ts.toInstant().toString());
        } else if (value instanceof LocalDateTime ldt) {
            generator.writeString(ldt.toString());
        } else if (value instanceof LocalDate ld) {
            generator.writeString(ld.toString());
        } else if (value instanceof OffsetDateTime odt) {
            generator.writeString(odt.toString());
        } else if (value instanceof byte[] bytes) {
            generator.writeBinary(bytes);
        } else {
            // Fallback to string representation
            generator.writeString(value.toString());
        }
    }

    @Override
    public void flush() throws IOException {
        if (generator != null) {
            generator.flush();
        }
    }

    @Override
    public void end() throws IOException {
        if (generator != null) {
            generator.writeEndArray();
            generator.flush();
            generator.close();
        }
        log.info("Completed JSON streaming: {} total rows", rowCount);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
