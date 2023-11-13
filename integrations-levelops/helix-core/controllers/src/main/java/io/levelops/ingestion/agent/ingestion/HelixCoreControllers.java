package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreDepot;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import io.levelops.integrations.helixcore.sources.HelixCoreChangeListDataSource;
import io.levelops.integrations.helixcore.sources.HelixCoreDepotDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

/**
 * Helixcore's implementation of the {@link DataController}
 */
@Log4j2
public class HelixCoreControllers {

    public static final String HELIX_CORE = "helix_core";
    public static final String DEPOTS = "depots";
    public static final String CHANGELIST = "changelists";

    @Builder(builderMethodName = "helixCoreDepotController", builderClassName = "HelixcoreDepotControllerBuilder")
    private static IntegrationController<HelixCoreIterativeQuery> buildDepotController(
            HelixCoreDepotDataSource helixCoreDepotDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<HelixCoreDepot, HelixCoreIterativeQuery>builder()
                .queryClass(HelixCoreIterativeQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<HelixCoreDepot, HelixCoreIterativeQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(helixCoreDepotDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(HELIX_CORE)
                        .dataType(DEPOTS)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "helixCoreChangeListController", builderClassName = "HelixcoreChangeListControllerBuilder")
    private static IntegrationController<HelixCoreIterativeQuery> buildChangeListController(
            HelixCoreChangeListDataSource helixCoreChangeListDataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper,
            @Nullable Integer outputPageSize) {
        return PaginatedIntegrationController.<HelixCoreChangeList, HelixCoreIterativeQuery>builder()
                .queryClass(HelixCoreIterativeQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<HelixCoreChangeList, HelixCoreIterativeQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(helixCoreChangeListDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(HELIX_CORE)
                        .dataType(CHANGELIST)
                        .outputPageSize(outputPageSize)
                        .build())
                .build();
    }
}
