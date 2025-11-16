package com.example.exporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the data exporter.
 */
@Component
@ConfigurationProperties(prefix = "exporter")
public class ExporterProperties {

    private int fetchSize = 10000;
    private int bufferSize = 65536;
    private CacheProperties cache = new CacheProperties();
    private StreamingProperties streaming = new StreamingProperties();

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public StreamingProperties getStreaming() {
        return streaming;
    }

    public void setStreaming(StreamingProperties streaming) {
        this.streaming = streaming;
    }

    public static class CacheProperties {
        private int maxSize = 1000;
        private int ttlSeconds = 3600;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class StreamingProperties {
        private int flushInterval = 1000;
        private boolean gzipEnabled = false;

        public int getFlushInterval() {
            return flushInterval;
        }

        public void setFlushInterval(int flushInterval) {
            this.flushInterval = flushInterval;
        }

        public boolean isGzipEnabled() {
            return gzipEnabled;
        }

        public void setGzipEnabled(boolean gzipEnabled) {
            this.gzipEnabled = gzipEnabled;
        }
    }
}
