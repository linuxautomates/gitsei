package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

@Log4j2
public class SnykIterativeScanController implements DataController<BaseIntegrationQuery> {
    private final ObjectMapper objectMapper;
    private final IntegrationController<BaseIntegrationQuery> orgsController;
    private final IntegrationController<BaseIntegrationQuery> projectsController;
    private final IntegrationController<BaseIntegrationQuery> issuesController;
    private final IntegrationController<BaseIntegrationQuery> depGraphController;


    @Builder
    public SnykIterativeScanController(ObjectMapper objectMapper,
                                       IntegrationController<BaseIntegrationQuery> orgsController,
                                       IntegrationController<BaseIntegrationQuery> projectsController,
                                       IntegrationController<BaseIntegrationQuery> issuesController,
                                       IntegrationController<BaseIntegrationQuery> depGraphController) {

        this.objectMapper = objectMapper;
        this.orgsController = orgsController;
        this.projectsController = projectsController;
        this.issuesController = issuesController;
        this.depGraphController = depGraphController;
    }


    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, BaseIntegrationQuery query) throws IngestException {

        // TODO iterative scan

        ControllerIngestionResult projectsResult = projectsController.ingest(jobContext, query);
        long nbProjects = CollectionUtils.size(((StorageResult) projectsResult).getRecords());
        log.info("Found {} Snyk projects for integration {}", nbProjects, query.getIntegrationKey());
        if (nbProjects <= 0) {
            // if there is no project, don't bother fetching anything else
            return new EmptyIngestionResult();
        }
        ControllerIngestionResult orgsResult = orgsController.ingest(jobContext, query);
        ControllerIngestionResult issuesResult = issuesController.ingest(jobContext, query);
        ControllerIngestionResult depGraphResult = depGraphController.ingest(jobContext, query);

        return new ControllerIngestionResultList(issuesResult, depGraphResult, orgsResult, projectsResult);
    }

    @Override
    public BaseIntegrationQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, BaseIntegrationQuery.class);
    }
}
