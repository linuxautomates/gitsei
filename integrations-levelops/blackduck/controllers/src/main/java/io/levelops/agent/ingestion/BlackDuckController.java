package io.levelops.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.blackduck.models.BlackDuckIterativeScanQuery;
import io.levelops.integrations.blackduck.models.EnrichedProjectData;
import io.levelops.sources.BlackDuckDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BlackDuckController {

    private static final String PROJECTS = "projects";

    @Builder(builderMethodName = "projectController", builderClassName = "BlackDuckProjectController")
    private static IntegrationController<BlackDuckIterativeScanQuery> buildProjectController(
            BlackDuckDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<EnrichedProjectData, BlackDuckIterativeScanQuery>builder()
                .queryClass(BlackDuckIterativeScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<EnrichedProjectData, BlackDuckIterativeScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(IntegrationType.BLACKDUCK.toString())
                        .dataType(PROJECTS)
                        .skipEmptyResults(true)
                        .build())
                .build();
    }

}
