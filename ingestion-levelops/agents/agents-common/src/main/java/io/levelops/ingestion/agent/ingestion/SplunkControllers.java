package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.PaginatedIntegrationController;
import io.levelops.ingestion.controllers.SinglePageIntegrationController;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.integrations.slack.models.SlackChatMessageQuery;
import io.levelops.ingestion.integrations.splunk.models.SplunkQuery;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.postgres.client.PostgresClient;
import io.levelops.integrations.postgres.sources.PostgresQueryDataSource;
import io.levelops.integrations.snyk.models.SnykOrg;
import io.levelops.integrations.snyk.sources.SnykOrgDataSource;
import io.levelops.integrations.splunk.sources.SplunkSearchDataSource;
import lombok.Builder;

public class SplunkControllers {
    @Builder(builderMethodName = "splunkSearchController", builderClassName = "SplunkSearchControllerBuilder")
    private static IntegrationController<SplunkSearchDataSource.SplunkSearchQuery> buildSplunkSearchControllerBuilder(
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SplunkSearchDataSource dataSource) {
        return PaginatedIntegrationController.<String, SplunkSearchDataSource.SplunkSearchQuery>builder()
                .queryClass(SplunkSearchDataSource.SplunkSearchQuery.class)
                .paginationStrategy(StreamedPaginationStrategy.<String, SplunkSearchDataSource.SplunkSearchQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(dataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType("postgres")
                        .dataType("query_results")
                        .build())
                .build();
    }
}
