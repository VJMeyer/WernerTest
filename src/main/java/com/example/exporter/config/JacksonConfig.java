package com.example.exporter.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for efficient JSON streaming.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonFactory jsonFactory() {
        return new JsonFactory();
    }

    @Bean
    public ObjectMapper objectMapper(JsonFactory jsonFactory) {
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.disable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
