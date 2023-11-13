package io.levelops.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.SinglePageStrategy;
import io.levelops.integrations.checkmarx.models.CxScaProject;
import io.levelops.integrations.checkmarx.sources.cxsca.CxScaProjectDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class CxScaProjectController implements DataController<CxScaProjectDataSource.CxScaProjectQuery> {

    public static final String PROJECTS = "projects";
    private SinglePageStrategy<CxScaProject, CxScaProjectDataSource.CxScaProjectQuery> cxScaProjectStrategy;
    private final ObjectMapper objectMapper;
    private final int onboardingScanInDays;

    @Builder
    public CxScaProjectController(ObjectMapper mapper, CxScaProjectDataSource dataSource, StorageDataSink sink,
                                  int onboardingScanInDays) {
        this.objectMapper = mapper;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.cxScaProjectStrategy = SinglePageStrategy.<CxScaProject, CxScaProjectDataSource.CxScaProjectQuery>builder()
                .objectMapper(objectMapper)
                .dataType(PROJECTS)
                .skipEmptyResults(true)
                .storageDataSink(sink)
                .dataSource(dataSource)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, CxScaProjectDataSource.CxScaProjectQuery query) throws IngestException {
        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(cxScaProjectStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query));
        return new ControllerIngestionResultList(results);
    }

    @Override
    public CxScaProjectDataSource.CxScaProjectQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        CxScaProjectDataSource.CxScaProjectQuery query = objectMapper.convertValue(arg, CxScaProjectDataSource.CxScaProjectQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }
}

