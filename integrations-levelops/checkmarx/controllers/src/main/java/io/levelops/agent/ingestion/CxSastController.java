package io.levelops.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastProjectDataSource;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastScanDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

/**
 * CxSast's implementation of the {@link DataController}
 */
@Log4j2
public class CxSastController {

    private static final String CXSAST = "cxsast";
    private static final String PROJECTS = "projects";
    private static final String SCANS = "scans";

    @Builder(builderMethodName = "projectController", builderClassName = "CxSastProjectController")
    private static IntegrationController<CxSastProjectDataSource.CxSastProjectQuery> buildProjectController(
            CxSastProjectDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<CxSastProject, CxSastProjectDataSource.CxSastProjectQuery>builder()
                .queryClass(CxSastProjectDataSource.CxSastProjectQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<CxSastProject, CxSastProjectDataSource.CxSastProjectQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(CXSAST)
                        .dataType(PROJECTS)
                        .build())
                .build();
    }

    @Builder(builderMethodName = "scanController", builderClassName = "CxSastScanController")
    private static IntegrationController<CxSastScanDataSource.CxSastScanQuery> buildScanController(CxSastScanDataSource dataSource,
                                                                                                   StorageDataSink storageDataSink,
                                                                                                   ObjectMapper objectMapper) {
        return PaginatedIntegrationController.<CxSastScan, CxSastScanDataSource.CxSastScanQuery>builder()
                .queryClass(CxSastScanDataSource.CxSastScanQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<CxSastScan, CxSastScanDataSource.CxSastScanQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(CXSAST)
                        .dataType(SCANS)
                        .build())
                .build();
    }

}
