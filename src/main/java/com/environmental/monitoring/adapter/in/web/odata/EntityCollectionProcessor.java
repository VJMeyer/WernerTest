package com.environmental.monitoring.adapter.in.web.odata;

import com.environmental.monitoring.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityCollectionProcessor implements org.apache.olingo.server.api.processor.EntityCollectionProcessor {

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
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        EntityCollection entityCollection = getData(edmEntitySet);

        ODataSerializer serializer = odata.createSerializer(responseFormat);

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
                .id(id)
                .contextURL(contextUrl)
                .build();

        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entityCollection, opts);
        InputStream serializedContent = serializerResult.getContent();

        response.setContent(serializedContent);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    private EntityCollection getData(EdmEntitySet edmEntitySet) throws ODataApplicationException {
        String entitySetName = edmEntitySet.getName();

        switch (entitySetName) {
            case EdmProvider.ES_MEASURING_SITES_NAME:
                return entityMapper.mapMeasuringSitesToCollection(measuringSiteUseCase.getAllMeasuringSites());

            case EdmProvider.ES_STATIONS_NAME:
                return entityMapper.mapStationsToCollection(stationUseCase.getAllStations());

            case EdmProvider.ES_PARAMETER_TYPES_NAME:
                return entityMapper.mapParameterTypesToCollection(parameterTypeUseCase.getAllParameterTypes());

            case EdmProvider.ES_SAMPLINGS_NAME:
                return entityMapper.mapSamplingsToCollection(samplingUseCase.getAllSamplings());

            case EdmProvider.ES_SAMPLES_NAME:
                return entityMapper.mapSamplesToCollection(sampleUseCase.getAllSamples());

            case EdmProvider.ES_SAMPLE_VALUES_NAME:
                return entityMapper.mapSampleValuesToCollection(sampleValueUseCase.getAllSampleValues());

            default:
                throw new ODataApplicationException("Entity set not found: " + entitySetName,
                        HttpStatusCode.NOT_FOUND.getStatusCode(), null);
        }
    }
}
