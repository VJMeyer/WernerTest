package com.environmental.monitoring.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sample_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "text_value", length = 500)
    private String textValue;

    @Column(name = "quality_code", length = 20)
    @Builder.Default
    private String qualityCode = "GOOD";

    @Column(name = "detection_limit")
    private Double detectionLimit;

    @Column(name = "below_detection_limit")
    @Builder.Default
    private Boolean belowDetectionLimit = false;

    @Column(length = 500)
    private String notes;

    @Column(name = "measurement_method", length = 100)
    private String measurementMethod;

    @Column(name = "uncertainty")
    private Double uncertainty;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_type_id", nullable = false)
    private ParameterType parameterType;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
