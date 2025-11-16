package de.kisters.hmt.cloud.services.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "de.kisters.hmt.cloud.services.upload",
    "de.kisters.hmt.cloud.messaging"
})
public class UploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UploadServiceApplication.class, args);
    }
}
