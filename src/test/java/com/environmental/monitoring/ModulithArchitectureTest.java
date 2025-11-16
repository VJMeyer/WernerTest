package com.environmental.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithArchitectureTest {

    ApplicationModules modules = ApplicationModules.of(EnvironmentalMonitoringApplication.class);

    @Test
    void shouldVerifyModuleStructure() {
        modules.verify();
    }

    @Test
    void shouldPrintModuleArrangement() {
        modules.forEach(System.out::println);
    }

    @Test
    void shouldGenerateDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

    @Test
    void shouldDetectAllModules() {
        System.out.println("=== Application Modules ===");
        modules.forEach(module -> {
            System.out.println("Module: " + module.getName());
            System.out.println("  Base Package: " + module.getBasePackage());
            System.out.println("  Dependencies: " + module.getDependencies());
            System.out.println();
        });
    }
}
