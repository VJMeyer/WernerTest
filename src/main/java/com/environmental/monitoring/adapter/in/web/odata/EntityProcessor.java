package com.environmental.monitoring.adapter.in.web.odata;

import com.environmental.monitoring.application.port.in.*;
import com.environmental.monitoring.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntityProcessor implements org.apache.olingo.server.api.processor.EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    private final MeasuringSiteUseCase measuringSiteUseCase;
    private final StationUseCase stationUseCase;
    private final ParameterTypeUseCase parameterTypeUseCase;
    private final SamplingUseCase samplingUseCase;
    private final SampleUseCase sampleUseCase;
    private final SampleValueUseCase sampleValueUseCase;
    private final EntityMapper entityMapper;

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        Entity entity = getData(edmEntitySet, keyPredicates);

        if (entity == null) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), null);
        }

        ODataSerializer serializer = odata.createSerializer(responseFormat);

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        EntitySerializerOptions options = EntitySerializerOptions.with()
                .contextURL(contextUrl)
                .build();

        SerializerResult serializerResult = serializer.entity(serviceMetadata, edmEntityType, entity, options);
        InputStream entityStream = serializerResult.getContent();

        response.setContent(entityStream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Create not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), null);
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Update not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), null);
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Delete not implemented",
                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), null);
    }

    private Entity getData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) throws ODataApplicationException {
        String entitySetName = edmEntitySet.getName();
        UUID id = extractId(keyPredicates);

        switch (entitySetName) {
            case EdmProvider.ES_MEASURING_SITES_NAME:
                return measuringSiteUseCase.getMeasuringSiteByIdWithStations(id)
                        .map(entityMapper::mapMeasuringSiteWithStations)
                        .orElse(null);

            case EdmProvider.ES_STATIONS_NAME:
                return stationUseCase.getStationByIdWithSamplings(id)
                        .map(entityMapper::mapStationWithSamplings)
                        .orElse(null);

            case EdmProvider.ES_PARAMETER_TYPES_NAME:
                return parameterTypeUseCase.getParameterTypeById(id)
                        .map(entityMapper::mapParameterType)
                        .orElse(null);

            case EdmProvider.ES_SAMPLINGS_NAME:
                return samplingUseCase.getSamplingByIdWithSamples(id)
                        .map(entityMapper::mapSamplingWithSamples)
                        .orElse(null);

            case EdmProvider.ES_SAMPLES_NAME:
                return sampleUseCase.getSampleByIdWithValues(id)
                        .map(entityMapper::mapSampleWithValues)
                        .orElse(null);

            case EdmProvider.ES_SAMPLE_VALUES_NAME:
                return sampleValueUseCase.getSampleValueById(id)
                        .map(entityMapper::mapSampleValue)
                        .orElse(null);

            default:
                throw new ODataApplicationException("Entity set not found: " + entitySetName,
                        HttpStatusCode.NOT_FOUND.getStatusCode(), null);
        }
    }

    private UUID extractId(List<UriParameter> keyPredicates) throws ODataApplicationException {
        for (UriParameter param : keyPredicates) {
            if ("Id".equals(param.getName())) {
                String idString = param.getText();
                try {
                    return UUID.fromString(idString);
                } catch (IllegalArgumentException e) {
                    throw new ODataApplicationException("Invalid UUID format",
                            HttpStatusCode.BAD_REQUEST.getStatusCode(), null);
                }
            }
        }
        throw new ODataApplicationException("Key parameter 'Id' not found",
                HttpStatusCode.BAD_REQUEST.getStatusCode(), null);
    }
}
