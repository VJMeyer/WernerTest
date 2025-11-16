package com.environmental.monitoring.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "samples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sample_code", unique = true, nullable = false, length = 50)
    private String sampleCode;

    @Column(name = "sample_type", length = 50)
    private String sampleType;

    @Column(name = "collection_time", nullable = false)
    private LocalDateTime collectionTime;

    @Column(name = "depth_meters")
    private Double depthMeters;

    @Column(length = 500)
    private String notes;

    @Column(name = "preservation_method", length = 100)
    private String preservationMethod;

    @Column(name = "container_type", length = 100)
    private String containerType;

    @Column(name = "quality_flag", length = 20)
    @Builder.Default
    private String qualityFlag = "VALID";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sampling_id", nullable = false)
    private Sampling sampling;

    @OneToMany(mappedBy = "sample", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SampleValue> sampleValues = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSampleValue(SampleValue sampleValue) {
        sampleValues.add(sampleValue);
        sampleValue.setSample(this);
    }

    public void removeSampleValue(SampleValue sampleValue) {
        sampleValues.remove(sampleValue);
        sampleValue.setSample(null);
    }
}
