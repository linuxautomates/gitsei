package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.coverity.models.EnrichedProjectData;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import io.levelops.integrations.coverity.sources.CoverityMergedDefectDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

/**
 * Coverity's implementation of the {@link DataController}
 */
@Log4j2
public class CoverityControllers {

    private static final String COVERITY = "CoverityIterativeScanController";
    private static final String DEFECTS_DATATYPE = "defects";

    @Builder(builderMethodName = "defectsController", builderClassName = "CoverityDefectsController")
    private static IntegrationController<CoverityIterativeScanQuery> buildDefectsController(
            CoverityMergedDefectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, CoverityIterativeScanQuery>builder()
                .queryClass(CoverityIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, CoverityIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(COVERITY)
                        .dataType(DEFECTS_DATATYPE)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }
}