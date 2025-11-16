package de.kisters.hmt.cloud.connector.wiski.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "de.kisters.hmt.cloud.connector.wiski.batch",
    "de.kisters.hmt.cloud.messaging"
})
public class BatchConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchConnectorApplication.class, args);
    }
}
