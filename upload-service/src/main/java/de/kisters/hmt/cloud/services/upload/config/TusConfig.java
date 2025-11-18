package de.kisters.hmt.cloud.services.upload.config;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class TusConfig {

    @Value("${tus.upload.directory}")
    private String tusUploadDirectory;

    @Value("${tus.upload.expiration.period:86400000}")
    private Long tusExpirationPeriod; // Default: 24 hours in milliseconds

    @Value("${tus.max.upload.size:104857600}")
    private Long tusMaxUploadSize; // Default: 100MB

    @Bean
    public TusFileUploadService tusFileUploadService() throws IOException {
        return new TusFileUploadService()
                .withStoragePath(tusUploadDirectory)
                .withDownloadFeature()
                .withUploadExpirationPeriod(tusExpirationPeriod)
                .withMaxUploadSize(tusMaxUploadSize)
                .withThreadLocalCache(true);
    }
}
