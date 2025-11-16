package com.environmental.monitoring.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "samplings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sampling {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sampling_code", unique = true, nullable = false, length = 50)
    private String samplingCode;

    @Column(name = "sampling_date", nullable = false)
    private LocalDateTime samplingDate;

    @Column(name = "sampler_name", length = 100)
    private String samplerName;

    @Column(length = 1000)
    private String notes;

    @Column(name = "weather_conditions", length = 200)
    private String weatherConditions;

    @Column(name = "air_temperature")
    private Double airTemperature;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @OneToMany(mappedBy = "sampling", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Sample> samples = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSample(Sample sample) {
        samples.add(sample);
        sample.setSampling(this);
    }

    public void removeSample(Sample sample) {
        samples.remove(sample);
        sample.setSampling(null);
    }
}
