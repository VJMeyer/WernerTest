package com.environmental.monitoring.adapter.in.web.odata;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class EdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "com.environmental.monitoring";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types
    public static final String ET_MEASURING_SITE_NAME = "MeasuringSite";
    public static final FullQualifiedName ET_MEASURING_SITE_FQN = new FullQualifiedName(NAMESPACE, ET_MEASURING_SITE_NAME);

    public static final String ET_STATION_NAME = "Station";
    public static final FullQualifiedName ET_STATION_FQN = new FullQualifiedName(NAMESPACE, ET_STATION_NAME);

    public static final String ET_PARAMETER_TYPE_NAME = "ParameterType";
    public static final FullQualifiedName ET_PARAMETER_TYPE_FQN = new FullQualifiedName(NAMESPACE, ET_PARAMETER_TYPE_NAME);

    public static final String ET_SAMPLING_NAME = "Sampling";
    public static final FullQualifiedName ET_SAMPLING_FQN = new FullQualifiedName(NAMESPACE, ET_SAMPLING_NAME);

    public static final String ET_SAMPLE_NAME = "Sample";
    public static final FullQualifiedName ET_SAMPLE_FQN = new FullQualifiedName(NAMESPACE, ET_SAMPLE_NAME);

    public static final String ET_SAMPLE_VALUE_NAME = "SampleValue";
    public static final FullQualifiedName ET_SAMPLE_VALUE_FQN = new FullQualifiedName(NAMESPACE, ET_SAMPLE_VALUE_NAME);

    // Entity Set Names
    public static final String ES_MEASURING_SITES_NAME = "MeasuringSites";
    public static final String ES_STATIONS_NAME = "Stations";
    public static final String ES_PARAMETER_TYPES_NAME = "ParameterTypes";
    public static final String ES_SAMPLINGS_NAME = "Samplings";
    public static final String ES_SAMPLES_NAME = "Samples";
    public static final String ES_SAMPLE_VALUES_NAME = "SampleValues";

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        if (entityTypeName.equals(ET_MEASURING_SITE_FQN)) {
            return getMeasuringSiteEntityType();
        } else if (entityTypeName.equals(ET_STATION_FQN)) {
            return getStationEntityType();
        } else if (entityTypeName.equals(ET_PARAMETER_TYPE_FQN)) {
            return getParameterTypeEntityType();
        } else if (entityTypeName.equals(ET_SAMPLING_FQN)) {
            return getSamplingEntityType();
        } else if (entityTypeName.equals(ET_SAMPLE_FQN)) {
            return getSampleEntityType();
        } else if (entityTypeName.equals(ET_SAMPLE_VALUE_FQN)) {
            return getSampleValueEntityType();
        }
        return null;
    }

    private CsdlEntityType getMeasuringSiteEntityType() {
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty description = new CsdlProperty().setName("Description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty latitude = new CsdlProperty().setName("Latitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty longitude = new CsdlProperty().setName("Longitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty address = new CsdlProperty().setName("Address").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty siteCode = new CsdlProperty().setName("SiteCode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty active = new CsdlProperty().setName("Active").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName());
        CsdlProperty createdAt = new CsdlProperty().setName("CreatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty updatedAt = new CsdlProperty().setName("UpdatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());

        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        propertyRef.setName("Id");

        CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                .setName("Stations")
                .setType(ET_STATION_FQN)
                .setCollection(true)
                .setPartner("MeasuringSite");

        return new CsdlEntityType()
                .setName(ET_MEASURING_SITE_NAME)
                .setProperties(Arrays.asList(id, name, description, latitude, longitude, address, siteCode, active, createdAt, updatedAt))
                .setKey(Collections.singletonList(propertyRef))
                .setNavigationProperties(Collections.singletonList(navProp));
    }

    private CsdlEntityType getStationEntityType() {
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty description = new CsdlProperty().setName("Description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty stationCode = new CsdlProperty().setName("StationCode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty stationType = new CsdlProperty().setName("StationType").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty latitude = new CsdlProperty().setName("Latitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty longitude = new CsdlProperty().setName("Longitude").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty elevationMeters = new CsdlProperty().setName("ElevationMeters").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty active = new CsdlProperty().setName("Active").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName());
        CsdlProperty installationDate = new CsdlProperty().setName("InstallationDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty createdAt = new CsdlProperty().setName("CreatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty updatedAt = new CsdlProperty().setName("UpdatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty measuringSiteId = new CsdlProperty().setName("MeasuringSiteId").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());

        CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName("Id");

        CsdlNavigationProperty navPropSite = new CsdlNavigationProperty()
                .setName("MeasuringSite")
                .setType(ET_MEASURING_SITE_FQN)
                .setNullable(false)
                .setPartner("Stations");

        CsdlNavigationProperty navPropSamplings = new CsdlNavigationProperty()
                .setName("Samplings")
                .setType(ET_SAMPLING_FQN)
                .setCollection(true)
                .setPartner("Station");

        return new CsdlEntityType()
                .setName(ET_STATION_NAME)
                .setProperties(Arrays.asList(id, name, description, stationCode, stationType, latitude, longitude, elevationMeters, active, installationDate, createdAt, updatedAt, measuringSiteId))
                .setKey(Collections.singletonList(propertyRef))
                .setNavigationProperties(Arrays.asList(navPropSite, navPropSamplings));
    }

    private CsdlEntityType getParameterTypeEntityType() {
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty description = new CsdlProperty().setName("Description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty parameterCode = new CsdlProperty().setName("ParameterCode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty unitOfMeasure = new CsdlProperty().setName("UnitOfMeasure").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty minValidValue = new CsdlProperty().setName("MinValidValue").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty maxValidValue = new CsdlProperty().setName("MaxValidValue").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty precisionDigits = new CsdlProperty().setName("PrecisionDigits").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty category = new CsdlProperty().setName("Category").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty active = new CsdlProperty().setName("Active").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName());
        CsdlProperty createdAt = new CsdlProperty().setName("CreatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty updatedAt = new CsdlProperty().setName("UpdatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());

        CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName("Id");

        return new CsdlEntityType()
                .setName(ET_PARAMETER_TYPE_NAME)
                .setProperties(Arrays.asList(id, name, description, parameterCode, unitOfMeasure, minValidValue, maxValidValue, precisionDigits, category, active, createdAt, updatedAt))
                .setKey(Collections.singletonList(propertyRef));
    }

    private CsdlEntityType getSamplingEntityType() {
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty samplingCode = new CsdlProperty().setName("SamplingCode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty samplingDate = new CsdlProperty().setName("SamplingDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty samplerName = new CsdlProperty().setName("SamplerName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty notes = new CsdlProperty().setName("Notes").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty weatherConditions = new CsdlProperty().setName("WeatherConditions").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty airTemperature = new CsdlProperty().setName("AirTemperature").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty status = new CsdlProperty().setName("Status").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty createdAt = new CsdlProperty().setName("CreatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty updatedAt = new CsdlProperty().setName("UpdatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty stationId = new CsdlProperty().setName("StationId").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());

        CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName("Id");

        CsdlNavigationProperty navPropStation = new CsdlNavigationProperty()
                .setName("Station")
                .setType(ET_STATION_FQN)
                .setNullable(false)
                .setPartner("Samplings");

        CsdlNavigationProperty navPropSamples = new CsdlNavigationProperty()
                .setName("Samples")
                .setType(ET_SAMPLE_FQN)
                .setCollection(true)
                .setPartner("Sampling");

        return new CsdlEntityType()
                .setName(ET_SAMPLING_NAME)
                .setProperties(Arrays.asList(id, samplingCode, samplingDate, samplerName, notes, weatherConditions, airTemperature, status, createdAt, updatedAt, stationId))
                .setKey(Collections.singletonList(propertyRef))
                .setNavigationProperties(Arrays.asList(navPropStation, navPropSamples));
    }

    private CsdlEntityType getSampleEntityType() {
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty sampleCode = new CsdlProperty().setName("SampleCode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty sampleType = new CsdlProperty().setName("SampleType").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty collectionTime = new CsdlProperty().setName("CollectionTime").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty depthMeters = new CsdlProperty().setName("DepthMeters").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty notes = new CsdlProperty().setName("Notes").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty preservationMethod = new CsdlProperty().setName("PreservationMethod").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty containerType = new CsdlProperty().setName("ContainerType").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty qualityFlag = new CsdlProperty().setName("QualityFlag").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty createdAt = new CsdlProperty().setName("CreatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty updatedAt = new CsdlProperty().setName("UpdatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty samplingId = new CsdlProperty().setName("SamplingId").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());

        CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName("Id");

        CsdlNavigationProperty navPropSampling = new CsdlNavigationProperty()
                .setName("Sampling")
                .setType(ET_SAMPLING_FQN)
                .setNullable(false)
                .setPartner("Samples");

        CsdlNavigationProperty navPropValues = new CsdlNavigationProperty()
                .setName("SampleValues")
                .setType(ET_SAMPLE_VALUE_FQN)
                .setCollection(true)
                .setPartner("Sample");

        return new CsdlEntityType()
                .setName(ET_SAMPLE_NAME)
                .setProperties(Arrays.asList(id, sampleCode, sampleType, collectionTime, depthMeters, notes, preservationMethod, containerType, qualityFlag, createdAt, updatedAt, samplingId))
                .setKey(Collections.singletonList(propertyRef))
                .setNavigationProperties(Arrays.asList(navPropSampling, navPropValues));
    }

    private CsdlEntityType getSampleValueEntityType() {
        CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty numericValue = new CsdlProperty().setName("NumericValue").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty textValue = new CsdlProperty().setName("TextValue").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty qualityCode = new CsdlProperty().setName("QualityCode").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty detectionLimit = new CsdlProperty().setName("DetectionLimit").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty belowDetectionLimit = new CsdlProperty().setName("BelowDetectionLimit").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName());
        CsdlProperty notes = new CsdlProperty().setName("Notes").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty measurementMethod = new CsdlProperty().setName("MeasurementMethod").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty uncertainty = new CsdlProperty().setName("Uncertainty").setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
        CsdlProperty createdAt = new CsdlProperty().setName("CreatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty updatedAt = new CsdlProperty().setName("UpdatedAt").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
        CsdlProperty sampleId = new CsdlProperty().setName("SampleId").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
        CsdlProperty parameterTypeId = new CsdlProperty().setName("ParameterTypeId").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());

        CsdlPropertyRef propertyRef = new CsdlPropertyRef().setName("Id");

        CsdlNavigationProperty navPropSample = new CsdlNavigationProperty()
                .setName("Sample")
                .setType(ET_SAMPLE_FQN)
                .setNullable(false)
                .setPartner("SampleValues");

        CsdlNavigationProperty navPropParameterType = new CsdlNavigationProperty()
                .setName("ParameterType")
                .setType(ET_PARAMETER_TYPE_FQN)
                .setNullable(false);

        return new CsdlEntityType()
                .setName(ET_SAMPLE_VALUE_NAME)
                .setProperties(Arrays.asList(id, numericValue, textValue, qualityCode, detectionLimit, belowDetectionLimit, notes, measurementMethod, uncertainty, createdAt, updatedAt, sampleId, parameterTypeId))
                .setKey(Collections.singletonList(propertyRef))
                .setNavigationProperties(Arrays.asList(navPropSample, navPropParameterType));
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if (entityContainer.equals(CONTAINER)) {
            switch (entitySetName) {
                case ES_MEASURING_SITES_NAME:
                    return new CsdlEntitySet()
                            .setName(ES_MEASURING_SITES_NAME)
                            .setType(ET_MEASURING_SITE_FQN)
                            .setNavigationPropertyBindings(Collections.singletonList(
                                    new CsdlNavigationPropertyBinding().setPath("Stations").setTarget(ES_STATIONS_NAME)));
                case ES_STATIONS_NAME:
                    return new CsdlEntitySet()
                            .setName(ES_STATIONS_NAME)
                            .setType(ET_STATION_FQN)
                            .setNavigationPropertyBindings(Arrays.asList(
                                    new CsdlNavigationPropertyBinding().setPath("MeasuringSite").setTarget(ES_MEASURING_SITES_NAME),
                                    new CsdlNavigationPropertyBinding().setPath("Samplings").setTarget(ES_SAMPLINGS_NAME)));
                case ES_PARAMETER_TYPES_NAME:
                    return new CsdlEntitySet()
                            .setName(ES_PARAMETER_TYPES_NAME)
                            .setType(ET_PARAMETER_TYPE_FQN);
                case ES_SAMPLINGS_NAME:
                    return new CsdlEntitySet()
                            .setName(ES_SAMPLINGS_NAME)
                            .setType(ET_SAMPLING_FQN)
                            .setNavigationPropertyBindings(Arrays.asList(
                                    new CsdlNavigationPropertyBinding().setPath("Station").setTarget(ES_STATIONS_NAME),
                                    new CsdlNavigationPropertyBinding().setPath("Samples").setTarget(ES_SAMPLES_NAME)));
                case ES_SAMPLES_NAME:
                    return new CsdlEntitySet()
                            .setName(ES_SAMPLES_NAME)
                            .setType(ET_SAMPLE_FQN)
                            .setNavigationPropertyBindings(Arrays.asList(
                                    new CsdlNavigationPropertyBinding().setPath("Sampling").setTarget(ES_SAMPLINGS_NAME),
                                    new CsdlNavigationPropertyBinding().setPath("SampleValues").setTarget(ES_SAMPLE_VALUES_NAME)));
                case ES_SAMPLE_VALUES_NAME:
                    return new CsdlEntitySet()
                            .setName(ES_SAMPLE_VALUES_NAME)
                            .setType(ET_SAMPLE_VALUE_FQN)
                            .setNavigationPropertyBindings(Arrays.asList(
                                    new CsdlNavigationPropertyBinding().setPath("Sample").setTarget(ES_SAMPLES_NAME),
                                    new CsdlNavigationPropertyBinding().setPath("ParameterType").setTarget(ES_PARAMETER_TYPES_NAME)));
            }
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getMeasuringSiteEntityType());
        entityTypes.add(getStationEntityType());
        entityTypes.add(getParameterTypeEntityType());
        entityTypes.add(getSamplingEntityType());
        entityTypes.add(getSampleEntityType());
        entityTypes.add(getSampleValueEntityType());
        schema.setEntityTypes(entityTypes);

        schema.setEntityContainer(getEntityContainer());

        return Collections.singletonList(schema);
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(CONTAINER, ES_MEASURING_SITES_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_STATIONS_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_PARAMETER_TYPES_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_SAMPLINGS_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_SAMPLES_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_SAMPLE_VALUES_NAME));

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }
}
