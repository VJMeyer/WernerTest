package com.environmental.monitoring.adapter.in.web.odata;

import com.environmental.monitoring.domain.model.*;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class EntityMapper {

    /**
     * Maps MeasuringSite to OData Entity (projection without collections)
     */
    public Entity mapMeasuringSiteToProjection(MeasuringSite site) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, site.getId()));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, site.getName()));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, site.getDescription()));
        entity.addProperty(new Property(null, "Latitude", ValueType.PRIMITIVE, site.getLatitude()));
        entity.addProperty(new Property(null, "Longitude", ValueType.PRIMITIVE, site.getLongitude()));
        entity.addProperty(new Property(null, "Address", ValueType.PRIMITIVE, site.getAddress()));
        entity.addProperty(new Property(null, "SiteCode", ValueType.PRIMITIVE, site.getSiteCode()));
        entity.addProperty(new Property(null, "Active", ValueType.PRIMITIVE, site.getActive()));
        entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE,
                site.getCreatedAt() != null ? site.getCreatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "UpdatedAt", ValueType.PRIMITIVE,
                site.getUpdatedAt() != null ? site.getUpdatedAt().toInstant(ZoneOffset.UTC) : null));

        entity.setId(createId(EdmProvider.ES_MEASURING_SITES_NAME, site.getId()));
        return entity;
    }

    /**
     * Maps MeasuringSite to OData Entity with embedded Stations collection
     */
    public Entity mapMeasuringSiteWithStations(MeasuringSite site) {
        Entity entity = mapMeasuringSiteToProjection(site);

        if (site.getStations() != null && !site.getStations().isEmpty()) {
            EntityCollection stationsCollection = new EntityCollection();
            for (Station station : site.getStations()) {
                stationsCollection.getEntities().add(mapStationToProjection(station));
            }
            entity.addProperty(new Property(null, "Stations", ValueType.COLLECTION_ENTITY, stationsCollection.getEntities()));
        }

        return entity;
    }

    /**
     * Maps Station to OData Entity (projection without collections)
     */
    public Entity mapStationToProjection(Station station) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, station.getId()));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, station.getName()));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, station.getDescription()));
        entity.addProperty(new Property(null, "StationCode", ValueType.PRIMITIVE, station.getStationCode()));
        entity.addProperty(new Property(null, "StationType", ValueType.PRIMITIVE, station.getStationType()));
        entity.addProperty(new Property(null, "Latitude", ValueType.PRIMITIVE, station.getLatitude()));
        entity.addProperty(new Property(null, "Longitude", ValueType.PRIMITIVE, station.getLongitude()));
        entity.addProperty(new Property(null, "ElevationMeters", ValueType.PRIMITIVE, station.getElevationMeters()));
        entity.addProperty(new Property(null, "Active", ValueType.PRIMITIVE, station.getActive()));
        entity.addProperty(new Property(null, "InstallationDate", ValueType.PRIMITIVE,
                station.getInstallationDate() != null ? station.getInstallationDate().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE,
                station.getCreatedAt() != null ? station.getCreatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "UpdatedAt", ValueType.PRIMITIVE,
                station.getUpdatedAt() != null ? station.getUpdatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "MeasuringSiteId", ValueType.PRIMITIVE,
                station.getMeasuringSite() != null ? station.getMeasuringSite().getId() : null));

        entity.setId(createId(EdmProvider.ES_STATIONS_NAME, station.getId()));
        return entity;
    }

    /**
     * Maps Station to OData Entity with embedded Samplings collection
     */
    public Entity mapStationWithSamplings(Station station) {
        Entity entity = mapStationToProjection(station);

        if (station.getSamplings() != null && !station.getSamplings().isEmpty()) {
            EntityCollection samplingsCollection = new EntityCollection();
            for (Sampling sampling : station.getSamplings()) {
                samplingsCollection.getEntities().add(mapSamplingToProjection(sampling));
            }
            entity.addProperty(new Property(null, "Samplings", ValueType.COLLECTION_ENTITY, samplingsCollection.getEntities()));
        }

        return entity;
    }

    /**
     * Maps ParameterType to OData Entity
     */
    public Entity mapParameterType(ParameterType parameterType) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, parameterType.getId()));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, parameterType.getName()));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, parameterType.getDescription()));
        entity.addProperty(new Property(null, "ParameterCode", ValueType.PRIMITIVE, parameterType.getParameterCode()));
        entity.addProperty(new Property(null, "UnitOfMeasure", ValueType.PRIMITIVE, parameterType.getUnitOfMeasure()));
        entity.addProperty(new Property(null, "MinValidValue", ValueType.PRIMITIVE, parameterType.getMinValidValue()));
        entity.addProperty(new Property(null, "MaxValidValue", ValueType.PRIMITIVE, parameterType.getMaxValidValue()));
        entity.addProperty(new Property(null, "PrecisionDigits", ValueType.PRIMITIVE, parameterType.getPrecisionDigits()));
        entity.addProperty(new Property(null, "Category", ValueType.PRIMITIVE, parameterType.getCategory()));
        entity.addProperty(new Property(null, "Active", ValueType.PRIMITIVE, parameterType.getActive()));
        entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE,
                parameterType.getCreatedAt() != null ? parameterType.getCreatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "UpdatedAt", ValueType.PRIMITIVE,
                parameterType.getUpdatedAt() != null ? parameterType.getUpdatedAt().toInstant(ZoneOffset.UTC) : null));

        entity.setId(createId(EdmProvider.ES_PARAMETER_TYPES_NAME, parameterType.getId()));
        return entity;
    }

    /**
     * Maps Sampling to OData Entity (projection without collections)
     */
    public Entity mapSamplingToProjection(Sampling sampling) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, sampling.getId()));
        entity.addProperty(new Property(null, "SamplingCode", ValueType.PRIMITIVE, sampling.getSamplingCode()));
        entity.addProperty(new Property(null, "SamplingDate", ValueType.PRIMITIVE,
                sampling.getSamplingDate() != null ? sampling.getSamplingDate().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "SamplerName", ValueType.PRIMITIVE, sampling.getSamplerName()));
        entity.addProperty(new Property(null, "Notes", ValueType.PRIMITIVE, sampling.getNotes()));
        entity.addProperty(new Property(null, "WeatherConditions", ValueType.PRIMITIVE, sampling.getWeatherConditions()));
        entity.addProperty(new Property(null, "AirTemperature", ValueType.PRIMITIVE, sampling.getAirTemperature()));
        entity.addProperty(new Property(null, "Status", ValueType.PRIMITIVE, sampling.getStatus()));
        entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE,
                sampling.getCreatedAt() != null ? sampling.getCreatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "UpdatedAt", ValueType.PRIMITIVE,
                sampling.getUpdatedAt() != null ? sampling.getUpdatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "StationId", ValueType.PRIMITIVE,
                sampling.getStation() != null ? sampling.getStation().getId() : null));

        entity.setId(createId(EdmProvider.ES_SAMPLINGS_NAME, sampling.getId()));
        return entity;
    }

    /**
     * Maps Sampling to OData Entity with embedded Samples collection
     */
    public Entity mapSamplingWithSamples(Sampling sampling) {
        Entity entity = mapSamplingToProjection(sampling);

        if (sampling.getSamples() != null && !sampling.getSamples().isEmpty()) {
            EntityCollection samplesCollection = new EntityCollection();
            for (Sample sample : sampling.getSamples()) {
                samplesCollection.getEntities().add(mapSampleToProjection(sample));
            }
            entity.addProperty(new Property(null, "Samples", ValueType.COLLECTION_ENTITY, samplesCollection.getEntities()));
        }

        return entity;
    }

    /**
     * Maps Sample to OData Entity (projection without collections)
     */
    public Entity mapSampleToProjection(Sample sample) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, sample.getId()));
        entity.addProperty(new Property(null, "SampleCode", ValueType.PRIMITIVE, sample.getSampleCode()));
        entity.addProperty(new Property(null, "SampleType", ValueType.PRIMITIVE, sample.getSampleType()));
        entity.addProperty(new Property(null, "CollectionTime", ValueType.PRIMITIVE,
                sample.getCollectionTime() != null ? sample.getCollectionTime().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "DepthMeters", ValueType.PRIMITIVE, sample.getDepthMeters()));
        entity.addProperty(new Property(null, "Notes", ValueType.PRIMITIVE, sample.getNotes()));
        entity.addProperty(new Property(null, "PreservationMethod", ValueType.PRIMITIVE, sample.getPreservationMethod()));
        entity.addProperty(new Property(null, "ContainerType", ValueType.PRIMITIVE, sample.getContainerType()));
        entity.addProperty(new Property(null, "QualityFlag", ValueType.PRIMITIVE, sample.getQualityFlag()));
        entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE,
                sample.getCreatedAt() != null ? sample.getCreatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "UpdatedAt", ValueType.PRIMITIVE,
                sample.getUpdatedAt() != null ? sample.getUpdatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "SamplingId", ValueType.PRIMITIVE,
                sample.getSampling() != null ? sample.getSampling().getId() : null));

        entity.setId(createId(EdmProvider.ES_SAMPLES_NAME, sample.getId()));
        return entity;
    }

    /**
     * Maps Sample to OData Entity with embedded SampleValues collection
     */
    public Entity mapSampleWithValues(Sample sample) {
        Entity entity = mapSampleToProjection(sample);

        if (sample.getSampleValues() != null && !sample.getSampleValues().isEmpty()) {
            EntityCollection valuesCollection = new EntityCollection();
            for (SampleValue value : sample.getSampleValues()) {
                valuesCollection.getEntities().add(mapSampleValue(value));
            }
            entity.addProperty(new Property(null, "SampleValues", ValueType.COLLECTION_ENTITY, valuesCollection.getEntities()));
        }

        return entity;
    }

    /**
     * Maps SampleValue to OData Entity
     */
    public Entity mapSampleValue(SampleValue sampleValue) {
        Entity entity = new Entity();
        entity.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, sampleValue.getId()));
        entity.addProperty(new Property(null, "NumericValue", ValueType.PRIMITIVE, sampleValue.getNumericValue()));
        entity.addProperty(new Property(null, "TextValue", ValueType.PRIMITIVE, sampleValue.getTextValue()));
        entity.addProperty(new Property(null, "QualityCode", ValueType.PRIMITIVE, sampleValue.getQualityCode()));
        entity.addProperty(new Property(null, "DetectionLimit", ValueType.PRIMITIVE, sampleValue.getDetectionLimit()));
        entity.addProperty(new Property(null, "BelowDetectionLimit", ValueType.PRIMITIVE, sampleValue.getBelowDetectionLimit()));
        entity.addProperty(new Property(null, "Notes", ValueType.PRIMITIVE, sampleValue.getNotes()));
        entity.addProperty(new Property(null, "MeasurementMethod", ValueType.PRIMITIVE, sampleValue.getMeasurementMethod()));
        entity.addProperty(new Property(null, "Uncertainty", ValueType.PRIMITIVE, sampleValue.getUncertainty()));
        entity.addProperty(new Property(null, "CreatedAt", ValueType.PRIMITIVE,
                sampleValue.getCreatedAt() != null ? sampleValue.getCreatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "UpdatedAt", ValueType.PRIMITIVE,
                sampleValue.getUpdatedAt() != null ? sampleValue.getUpdatedAt().toInstant(ZoneOffset.UTC) : null));
        entity.addProperty(new Property(null, "SampleId", ValueType.PRIMITIVE,
                sampleValue.getSample() != null ? sampleValue.getSample().getId() : null));
        entity.addProperty(new Property(null, "ParameterTypeId", ValueType.PRIMITIVE,
                sampleValue.getParameterType() != null ? sampleValue.getParameterType().getId() : null));

        entity.setId(createId(EdmProvider.ES_SAMPLE_VALUES_NAME, sampleValue.getId()));
        return entity;
    }

    public EntityCollection mapMeasuringSitesToCollection(List<MeasuringSite> sites) {
        EntityCollection collection = new EntityCollection();
        for (MeasuringSite site : sites) {
            collection.getEntities().add(mapMeasuringSiteToProjection(site));
        }
        return collection;
    }

    public EntityCollection mapStationsToCollection(List<Station> stations) {
        EntityCollection collection = new EntityCollection();
        for (Station station : stations) {
            collection.getEntities().add(mapStationToProjection(station));
        }
        return collection;
    }

    public EntityCollection mapParameterTypesToCollection(List<ParameterType> parameterTypes) {
        EntityCollection collection = new EntityCollection();
        for (ParameterType type : parameterTypes) {
            collection.getEntities().add(mapParameterType(type));
        }
        return collection;
    }

    public EntityCollection mapSamplingsToCollection(List<Sampling> samplings) {
        EntityCollection collection = new EntityCollection();
        for (Sampling sampling : samplings) {
            collection.getEntities().add(mapSamplingToProjection(sampling));
        }
        return collection;
    }

    public EntityCollection mapSamplesToCollection(List<Sample> samples) {
        EntityCollection collection = new EntityCollection();
        for (Sample sample : samples) {
            collection.getEntities().add(mapSampleToProjection(sample));
        }
        return collection;
    }

    public EntityCollection mapSampleValuesToCollection(List<SampleValue> sampleValues) {
        EntityCollection collection = new EntityCollection();
        for (SampleValue value : sampleValues) {
            collection.getEntities().add(mapSampleValue(value));
        }
        return collection;
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + id + ")");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
