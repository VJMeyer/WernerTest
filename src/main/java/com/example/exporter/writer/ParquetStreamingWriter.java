package com.example.exporter.writer;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Streams data to Parquet format.
 *
 * Parquet is a columnar storage format optimized for analytical queries.
 * Since Parquet requires writing metadata at the end of the file, this
 * implementation writes to a temporary file first, then streams the result.
 *
 * Benefits of Parquet:
 * - Excellent compression (typically 2-10x smaller than JSON/CSV)
 * - Columnar storage for efficient analytical queries
 * - Schema enforcement
 * - Predicate pushdown support in query engines
 *
 * Note: This writer has higher memory usage than JSON/CSV writers due to
 * Parquet's need to buffer data for columnar organization. Configure
 * row group size appropriately for your memory constraints.
 */
public class ParquetStreamingWriter implements StreamingWriter {

    private static final Logger log = LoggerFactory.getLogger(ParquetStreamingWriter.class);

    private final int rowGroupSize;
    private final CompressionCodecName compressionCodec;

    private List<String> columns;
    private Schema avroSchema;
    private ParquetWriter<GenericRecord> parquetWriter;
    private java.nio.file.Path tempFile;
    private OutputStream outputStream;
    private long rowCount = 0;
    private Map<String, Schema.Type> columnTypes;

    /**
     * Create a Parquet writer with default settings.
     */
    public ParquetStreamingWriter() {
        this(100000, CompressionCodecName.SNAPPY);
    }

    /**
     * Create a Parquet writer with custom settings.
     *
     * @param rowGroupSize Number of rows per row group (affects memory and query performance)
     * @param compressionCodec Compression codec to use (SNAPPY, GZIP, ZSTD, etc.)
     */
    public ParquetStreamingWriter(int rowGroupSize, CompressionCodecName compressionCodec) {
        this.rowGroupSize = rowGroupSize;
        this.compressionCodec = compressionCodec;
        this.columnTypes = new HashMap<>();
    }

    @Override
    public void start(OutputStream outputStream, List<String> columns) throws IOException {
        this.outputStream = outputStream;
        this.columns = columns;

        // Create temporary file for Parquet output
        this.tempFile = Files.createTempFile("parquet-export-", ".parquet");
        log.debug("Created temporary Parquet file: {}", tempFile);

        // Schema will be built on first row when we know the types
        this.avroSchema = null;
        this.parquetWriter = null;

        log.debug("Started Parquet streaming with {} columns", columns.size());
    }

    @Override
    public void writeRow(Map<String, Object> row) throws IOException {
        // Build schema on first row
        if (avroSchema == null) {
            avroSchema = buildSchemaFromRow(row);
            initializeParquetWriter();
        }

        // Create Avro record
        GenericRecord record = new GenericData.Record(avroSchema);
        for (String column : columns) {
            Object value = row.get(column);
            Object avroValue = convertToAvroValue(value, columnTypes.get(column));
            record.put(column, avroValue);
        }

        parquetWriter.write(record);
        rowCount++;

        if (rowCount % 100000 == 0) {
            log.debug("Written {} rows to Parquet", rowCount);
        }
    }

    private Schema buildSchemaFromRow(Map<String, Object> row) {
        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record("ExportRecord")
                .namespace("com.example.exporter")
                .fields();

        for (String column : columns) {
            Object value = row.get(column);
            Schema.Type avroType = inferAvroType(value);
            columnTypes.put(column, avroType);

            // Make all fields nullable
            switch (avroType) {
                case STRING -> fieldAssembler.optionalString(column);
                case LONG -> fieldAssembler.optionalLong(column);
                case INT -> fieldAssembler.optionalInt(column);
                case DOUBLE -> fieldAssembler.optionalDouble(column);
                case FLOAT -> fieldAssembler.optionalFloat(column);
                case BOOLEAN -> fieldAssembler.optionalBoolean(column);
                case BYTES -> fieldAssembler.optionalBytes(column);
                default -> fieldAssembler.optionalString(column);
            }
        }

        Schema schema = fieldAssembler.endRecord();
        log.debug("Built Avro schema: {}", schema.toString(true));
        return schema;
    }

    private Schema.Type inferAvroType(Object value) {
        if (value == null) {
            return Schema.Type.STRING; // Default to string for null
        } else if (value instanceof String) {
            return Schema.Type.STRING;
        } else if (value instanceof Long) {
            return Schema.Type.LONG;
        } else if (value instanceof Integer) {
            return Schema.Type.INT;
        } else if (value instanceof Double) {
            return Schema.Type.DOUBLE;
        } else if (value instanceof Float) {
            return Schema.Type.FLOAT;
        } else if (value instanceof BigDecimal) {
            return Schema.Type.DOUBLE; // Store as double for simplicity
        } else if (value instanceof Boolean) {
            return Schema.Type.BOOLEAN;
        } else if (value instanceof byte[]) {
            return Schema.Type.BYTES;
        } else if (value instanceof Timestamp || value instanceof LocalDateTime ||
                value instanceof LocalDate || value instanceof OffsetDateTime) {
            return Schema.Type.STRING; // Store temporal types as ISO strings
        } else {
            return Schema.Type.STRING;
        }
    }

    private Object convertToAvroValue(Object value, Schema.Type targetType) {
        if (value == null) {
            return null;
        }

        return switch (targetType) {
            case STRING -> {
                if (value instanceof Timestamp ts) {
                    yield ts.toInstant().toString();
                } else if (value instanceof LocalDateTime ldt) {
                    yield ldt.toString();
                } else if (value instanceof LocalDate ld) {
                    yield ld.toString();
                } else if (value instanceof OffsetDateTime odt) {
                    yield odt.toString();
                } else {
                    yield value.toString();
                }
            }
            case LONG -> {
                if (value instanceof Long l) {
                    yield l;
                } else if (value instanceof Integer i) {
                    yield i.longValue();
                } else {
                    yield Long.parseLong(value.toString());
                }
            }
            case INT -> {
                if (value instanceof Integer i) {
                    yield i;
                } else {
                    yield Integer.parseInt(value.toString());
                }
            }
            case DOUBLE -> {
                if (value instanceof Double d) {
                    yield d;
                } else if (value instanceof BigDecimal bd) {
                    yield bd.doubleValue();
                } else if (value instanceof Float f) {
                    yield f.doubleValue();
                } else {
                    yield Double.parseDouble(value.toString());
                }
            }
            case FLOAT -> {
                if (value instanceof Float f) {
                    yield f;
                } else {
                    yield Float.parseFloat(value.toString());
                }
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b) {
                    yield b;
                } else {
                    yield Boolean.parseBoolean(value.toString());
                }
            }
            case BYTES -> {
                if (value instanceof byte[] bytes) {
                    yield java.nio.ByteBuffer.wrap(bytes);
                } else {
                    yield java.nio.ByteBuffer.wrap(value.toString().getBytes());
                }
            }
            default -> value.toString();
        };
    }

    private void initializeParquetWriter() throws IOException {
        Configuration conf = new Configuration();
        // Disable file system caching to avoid issues
        conf.set("fs.file.impl.disable.cache", "true");

        Path hadoopPath = new Path(tempFile.toUri());

        parquetWriter = AvroParquetWriter.<GenericRecord>builder(hadoopPath)
                .withSchema(avroSchema)
                .withConf(conf)
                .withCompressionCodec(compressionCodec)
                .withRowGroupSize(rowGroupSize * 1024L) // Convert to bytes (approximate)
                .withPageSize(1024 * 1024) // 1MB page size
                .withDictionaryEncoding(true)
                .withValidation(false)
                .withWriteMode(org.apache.parquet.hadoop.ParquetFileWriter.Mode.OVERWRITE)
                .build();

        log.debug("Initialized Parquet writer with compression: {}, row group size: {}",
                compressionCodec, rowGroupSize);
    }

    @Override
    public void flush() throws IOException {
        // Parquet handles its own flushing
    }

    @Override
    public void end() throws IOException {
        if (parquetWriter != null) {
            parquetWriter.close();
        }

        // Stream the Parquet file to the output
        if (tempFile != null && Files.exists(tempFile)) {
            long fileSize = Files.size(tempFile);
            log.info("Streaming Parquet file ({} bytes, {} rows) to output", fileSize, rowCount);

            try (var inputStream = Files.newInputStream(tempFile)) {
                byte[] buffer = new byte[65536];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } finally {
                // Clean up temporary file
                Files.deleteIfExists(tempFile);
                log.debug("Deleted temporary Parquet file");
            }
        }

        log.info("Completed Parquet streaming: {} total rows", rowCount);
    }

    @Override
    public String getContentType() {
        return "application/vnd.apache.parquet";
    }

    /**
     * Get the compression codec being used.
     */
    public CompressionCodecName getCompressionCodec() {
        return compressionCodec;
    }

    /**
     * Get the row group size.
     */
    public int getRowGroupSize() {
        return rowGroupSize;
    }
}
