package com.environmental.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(
    systemName = "Environmental Monitoring",
    sharedModules = {"domain"}
)
public class EnvironmentalMonitoringApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(EnvironmentalMonitoringApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(EnvironmentalMonitoringApplication.class);
    }
}
