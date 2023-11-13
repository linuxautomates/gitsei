package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.rapid7.Rapid7Query;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.strategies.pagination.NumberedPaginationStrategy;
import io.levelops.integrations.rapid7.models.Rapid7Vulnerability;
import io.levelops.integrations.rapid7.sources.Rapid7VulnerabilityDataSource;
import io.levelops.integrations.rapid7.sources.Rapid7VulnerabilityDataSource.Rapid7VulnerabilityQuery;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.ingestion.sinks.StorageDataSink;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

@Log4j2
public class Rapid7Controller implements DataController<Rapid7Query> {

    private final ObjectMapper objectMapper;

    private final NumberedPaginationStrategy<Rapid7Vulnerability, Rapid7VulnerabilityQuery> vulnerabilityPaginationStrategy;
    private final int onboardingInDays;

    @Builder
    public Rapid7Controller(ObjectMapper objectMapper,
                            StorageDataSink storageDataSink,
                            Rapid7VulnerabilityDataSource vulnerabilityDataSource,
                            @Nullable Integer onboardingInDays,
                            @Nullable Integer outputPageSize) {
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, 90);
        this.objectMapper = objectMapper;

        vulnerabilityPaginationStrategy = NumberedPaginationStrategy.<Rapid7Vulnerability, Rapid7VulnerabilityQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .integrationType("rapid7")
                .dataType("vulnerabilities")
                .pageDataSupplier((query, page) -> vulnerabilityDataSource.fetchMany(Rapid7VulnerabilityQuery.builder()
                        .integrationKey(query.getIntegrationKey())
                        .pageNumber(page.getPageNumber())
                        .build()))
                .outputPageSize(outputPageSize)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, Rapid7Query query) throws IngestException {
        // TODO iterative scan
        StorageResult vulnerabilityResult = vulnerabilityPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(),  Rapid7VulnerabilityQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .build());

        // TODO add more results

        return new ControllerIngestionResultList(vulnerabilityResult);
    }

    @Override
    public Rapid7Query parseQuery(Object o) {
        return objectMapper.convertValue(o, Rapid7Query.class);
    }

}
