package io.levelops.ingestion.controllers.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.GenericIntegrationQuery;
import io.levelops.ingestion.models.GenericMultiIntegrationQuery;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class GenericMultiIntegrationController implements DataController<GenericMultiIntegrationQuery> {

    private final ObjectMapper objectMapper;
    private final GenericIntegrationController genericIntegrationController;

    @Builder
    public GenericMultiIntegrationController(ObjectMapper objectMapper,
                                             GenericIntegrationController genericIntegrationController) {
        this.objectMapper = objectMapper;
        this.genericIntegrationController = genericIntegrationController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GenericMultiIntegrationQuery query) throws IngestException {
        Validate.notNull(jobContext, "jobContext cannot be null.");
        Validate.notBlank(jobContext.getJobId(), "jobId cannot be null or empty.");
        Validate.notNull(query, "query cannot be null.");

        List<ControllerIngestionResult> results = new ArrayList<>();
        for (GenericIntegrationQuery subQuery : query.getQueries()) {
            ControllerIngestionResult result = genericIntegrationController.ingest(jobContext, subQuery);
            results.add(result);
        }

        return new GenericMultiIntegrationResult(results);
    }

    @Override
    public GenericMultiIntegrationQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GenericMultiIntegrationQuery.class);
    }

    public static class GenericMultiIntegrationResult extends ListResponse<ControllerIngestionResult> implements ControllerIngestionResult {

        public GenericMultiIntegrationResult(List<ControllerIngestionResult> records) {
            super(records);
        }
    }

}
