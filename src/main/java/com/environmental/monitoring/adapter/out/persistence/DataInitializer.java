package com.environmental.monitoring.adapter.out.persistence;

import com.environmental.monitoring.application.port.in.*;
import com.environmental.monitoring.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("!production")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MeasuringSiteUseCase measuringSiteUseCase;
    private final StationUseCase stationUseCase;
    private final ParameterTypeUseCase parameterTypeUseCase;
    private final SamplingUseCase samplingUseCase;
    private final SampleUseCase sampleUseCase;
    private final SampleValueUseCase sampleValueUseCase;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data for development environment...");

        // Create Parameter Types
        ParameterType temperature = parameterTypeUseCase.createParameterType(
                ParameterType.builder()
                        .name("Temperature")
                        .description("Water temperature measurement")
                        .parameterCode("TEMP")
                        .unitOfMeasure("°C")
                        .minValidValue(-10.0)
                        .maxValidValue(50.0)
                        .precisionDigits(2)
                        .category("Physical")
                        .build()
        );

        ParameterType ph = parameterTypeUseCase.createParameterType(
                ParameterType.builder()
                        .name("pH")
                        .description("Acidity/Alkalinity measurement")
                        .parameterCode("PH")
                        .unitOfMeasure("pH units")
                        .minValidValue(0.0)
                        .maxValidValue(14.0)
                        .precisionDigits(2)
                        .category("Chemical")
                        .build()
        );

        ParameterType dissolvedOxygen = parameterTypeUseCase.createParameterType(
                ParameterType.builder()
                        .name("Dissolved Oxygen")
                        .description("Oxygen dissolved in water")
                        .parameterCode("DO")
                        .unitOfMeasure("mg/L")
                        .minValidValue(0.0)
                        .maxValidValue(20.0)
                        .precisionDigits(2)
                        .category("Chemical")
                        .build()
        );

        ParameterType turbidity = parameterTypeUseCase.createParameterType(
                ParameterType.builder()
                        .name("Turbidity")
                        .description("Water clarity measurement")
                        .parameterCode("TURB")
                        .unitOfMeasure("NTU")
                        .minValidValue(0.0)
                        .maxValidValue(1000.0)
                        .precisionDigits(1)
                        .category("Physical")
                        .build()
        );

        log.info("Created {} parameter types", 4);

        // Create Measuring Sites
        MeasuringSite riverSite = measuringSiteUseCase.createMeasuringSite(
                MeasuringSite.builder()
                        .name("Rhine River Monitoring Site")
                        .description("Main monitoring site along the Rhine River")
                        .latitude(50.9375)
                        .longitude(6.9603)
                        .address("Rhine River Bank, Cologne, Germany")
                        .siteCode("RHINE-001")
                        .build()
        );

        MeasuringSite lakeSite = measuringSiteUseCase.createMeasuringSite(
                MeasuringSite.builder()
                        .name("Lake Constance Monitoring Site")
                        .description("Water quality monitoring at Lake Constance")
                        .latitude(47.6500)
                        .longitude(9.1833)
                        .address("Lake Constance Shore, Konstanz, Germany")
                        .siteCode("LAKE-001")
                        .build()
        );

        log.info("Created {} measuring sites", 2);

        // Create Stations
        Station riverStation1 = stationUseCase.createStation(
                Station.builder()
                        .name("Rhine Station Alpha")
                        .description("Primary monitoring station on Rhine")
                        .stationCode("RHINE-ST-A")
                        .stationType("WATER_QUALITY")
                        .latitude(50.9380)
                        .longitude(6.9610)
                        .elevationMeters(45.0)
                        .installationDate(LocalDateTime.of(2020, 1, 15, 10, 0))
                        .measuringSite(riverSite)
                        .build()
        );

        Station riverStation2 = stationUseCase.createStation(
                Station.builder()
                        .name("Rhine Station Beta")
                        .description("Secondary monitoring station on Rhine")
                        .stationCode("RHINE-ST-B")
                        .stationType("WATER_QUALITY")
                        .latitude(50.9370)
                        .longitude(6.9595)
                        .elevationMeters(44.5)
                        .installationDate(LocalDateTime.of(2020, 3, 20, 14, 30))
                        .measuringSite(riverSite)
                        .build()
        );

        Station lakeStation1 = stationUseCase.createStation(
                Station.builder()
                        .name("Lake Station Gamma")
                        .description("Deep water monitoring station")
                        .stationCode("LAKE-ST-G")
                        .stationType("DEEP_WATER")
                        .latitude(47.6510)
                        .longitude(9.1840)
                        .elevationMeters(395.0)
                        .installationDate(LocalDateTime.of(2021, 6, 10, 9, 0))
                        .measuringSite(lakeSite)
                        .build()
        );

        log.info("Created {} stations", 3);

        // Create Samplings
        Sampling sampling1 = samplingUseCase.createSampling(
                Sampling.builder()
                        .samplingCode("SAMP-2024-001")
                        .samplingDate(LocalDateTime.of(2024, 1, 15, 9, 30))
                        .samplerName("Dr. Hans Mueller")
                        .notes("Regular monthly sampling")
                        .weatherConditions("Clear sky, light wind")
                        .airTemperature(12.5)
                        .status("COMPLETED")
                        .station(riverStation1)
                        .build()
        );

        Sampling sampling2 = samplingUseCase.createSampling(
                Sampling.builder()
                        .samplingCode("SAMP-2024-002")
                        .samplingDate(LocalDateTime.of(2024, 2, 15, 10, 0))
                        .samplerName("Dr. Anna Schmidt")
                        .notes("Post-storm sampling")
                        .weatherConditions("Overcast, after rain")
                        .airTemperature(8.0)
                        .status("COMPLETED")
                        .station(riverStation1)
                        .build()
        );

        Sampling sampling3 = samplingUseCase.createSampling(
                Sampling.builder()
                        .samplingCode("SAMP-2024-003")
                        .samplingDate(LocalDateTime.of(2024, 3, 10, 11, 0))
                        .samplerName("Dr. Klaus Weber")
                        .notes("Lake depth profile sampling")
                        .weatherConditions("Sunny, calm")
                        .airTemperature(15.0)
                        .status("PENDING")
                        .station(lakeStation1)
                        .build()
        );

        log.info("Created {} samplings", 3);

        // Create Samples
        Sample sample1 = sampleUseCase.createSample(
                Sample.builder()
                        .sampleCode("SAM-001-A")
                        .sampleType("SURFACE_WATER")
                        .collectionTime(LocalDateTime.of(2024, 1, 15, 9, 35))
                        .depthMeters(0.5)
                        .notes("Surface water sample")
                        .preservationMethod("COLD_STORAGE")
                        .containerType("GLASS_BOTTLE")
                        .qualityFlag("VALID")
                        .sampling(sampling1)
                        .build()
        );

        Sample sample2 = sampleUseCase.createSample(
                Sample.builder()
                        .sampleCode("SAM-001-B")
                        .sampleType("MID_DEPTH")
                        .collectionTime(LocalDateTime.of(2024, 1, 15, 9, 45))
                        .depthMeters(2.0)
                        .notes("Mid-depth water sample")
                        .preservationMethod("COLD_STORAGE")
                        .containerType("GLASS_BOTTLE")
                        .qualityFlag("VALID")
                        .sampling(sampling1)
                        .build()
        );

        Sample sample3 = sampleUseCase.createSample(
                Sample.builder()
                        .sampleCode("SAM-002-A")
                        .sampleType("SURFACE_WATER")
                        .collectionTime(LocalDateTime.of(2024, 2, 15, 10, 5))
                        .depthMeters(0.3)
                        .notes("Post-storm surface sample")
                        .preservationMethod("COLD_STORAGE")
                        .containerType("PLASTIC_BOTTLE")
                        .qualityFlag("VALID")
                        .sampling(sampling2)
                        .build()
        );

        log.info("Created {} samples", 3);

        // Create Sample Values
        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(18.5)
                        .qualityCode("GOOD")
                        .measurementMethod("THERMOMETER")
                        .uncertainty(0.1)
                        .sample(sample1)
                        .parameterType(temperature)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(7.2)
                        .qualityCode("GOOD")
                        .measurementMethod("PH_METER")
                        .uncertainty(0.05)
                        .sample(sample1)
                        .parameterType(ph)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(8.9)
                        .qualityCode("GOOD")
                        .measurementMethod("DO_PROBE")
                        .uncertainty(0.2)
                        .sample(sample1)
                        .parameterType(dissolvedOxygen)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(12.3)
                        .qualityCode("GOOD")
                        .measurementMethod("TURBIDITY_METER")
                        .uncertainty(0.5)
                        .sample(sample1)
                        .parameterType(turbidity)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(17.8)
                        .qualityCode("GOOD")
                        .measurementMethod("THERMOMETER")
                        .uncertainty(0.1)
                        .sample(sample2)
                        .parameterType(temperature)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(7.1)
                        .qualityCode("GOOD")
                        .measurementMethod("PH_METER")
                        .uncertainty(0.05)
                        .sample(sample2)
                        .parameterType(ph)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(10.5)
                        .qualityCode("GOOD")
                        .measurementMethod("THERMOMETER")
                        .uncertainty(0.1)
                        .sample(sample3)
                        .parameterType(temperature)
                        .build()
        );

        sampleValueUseCase.createSampleValue(
                SampleValue.builder()
                        .numericValue(45.8)
                        .qualityCode("QUESTIONABLE")
                        .notes("High turbidity after storm")
                        .measurementMethod("TURBIDITY_METER")
                        .uncertainty(1.0)
                        .sample(sample3)
                        .parameterType(turbidity)
                        .build()
        );

        log.info("Created {} sample values", 8);
        log.info("Sample data initialization completed successfully!");
    }
}
