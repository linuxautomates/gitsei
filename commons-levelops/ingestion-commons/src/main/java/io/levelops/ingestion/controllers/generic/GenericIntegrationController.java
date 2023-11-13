package io.levelops.ingestion.controllers.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.GenericIntegrationQuery;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import lombok.Singular;
import org.apache.commons.lang3.Validate;

import java.util.Set;

public class GenericIntegrationController implements DataController<GenericIntegrationQuery> {

    private final ObjectMapper objectMapper;
    private final Set<IntegrationController<?>> controllers;

    @Builder
    public GenericIntegrationController(ObjectMapper objectMapper,
                                        @Singular Set<IntegrationController<?>> controllers) {
        this.objectMapper = objectMapper;
        this.controllers = controllers;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ControllerIngestionResult ingest(JobContext jobContext, GenericIntegrationQuery query) throws IngestException {
        Validate.notNull(jobContext, "jobContext cannot be null.");
        Validate.notBlank(jobContext.getJobId(), "jobId cannot be null or empty.");
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notBlank(query.getIntegrationType(), "query.getIntegrationType() cannot be null or empty.");
        Validate.notBlank(query.getDataType(), "query.getDataType() cannot be null or empty.");

        IntegrationController controller = controllers.stream()
                .filter(c -> c.getIntegrationType().equals(query.getIntegrationType()))
                .filter(c -> c.getDataType().equals(query.getDataType()))
                .findAny()
                .orElseThrow(() -> new IngestException("Could not find controller for " + query));
        return controller.ingest(jobContext, controller.parseQuery(query));
    }

    @Override
    public GenericIntegrationQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GenericIntegrationQuery.class);
    }

}
