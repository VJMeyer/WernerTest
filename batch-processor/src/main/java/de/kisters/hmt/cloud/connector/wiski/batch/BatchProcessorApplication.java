package de.kisters.hmt.cloud.connector.wiski.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "de.kisters.hmt.cloud.connector.wiski.batch",
    "de.kisters.hmt.cloud.messaging"
})
public class BatchProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchProcessorApplication.class, args);
    }
}
