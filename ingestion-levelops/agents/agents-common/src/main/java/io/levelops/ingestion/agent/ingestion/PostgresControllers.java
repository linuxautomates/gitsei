package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.postgres.client.PostgresClient;
import io.levelops.integrations.postgres.sources.PostgresQueryDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PostgresControllers {
    @Builder(builderMethodName = "postgresController", builderClassName = "PostgresControllerBuilder")
    private static IntegrationController<PostgresQueryDataSource.PostgresQuery> buildPostgresController(
            PostgresQueryDataSource dataSource,
            StorageDataSink storageDataSink,
            ObjectMapper objectMapper) {

        return PaginatedIntegrationController.<PostgresClient.Row, PostgresQueryDataSource.PostgresQuery>builder()
                .queryClass(PostgresQueryDataSource.PostgresQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<PostgresClient.Row, PostgresQueryDataSource.PostgresQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("postgres")
                        .dataType("query_results")
                        .build())
                .build();
    }
}
