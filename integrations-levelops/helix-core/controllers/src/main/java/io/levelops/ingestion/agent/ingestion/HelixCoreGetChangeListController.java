package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import io.levelops.integrations.helixcore.sources.HelixCoreChangeListDataSource;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

@Log4j2
public class HelixCoreGetChangeListController implements DataController<HelixCoreIterativeQuery> {


    private final ObjectMapper objectMapper;
    private final HelixCoreChangeListDataSource helixCoreChangeListDataSource;

    @Builder
    public HelixCoreGetChangeListController(ObjectMapper objectMapper,
                                            HelixCoreChangeListDataSource helixCoreChangeListDataSource) {
        this.objectMapper = objectMapper;
        this.helixCoreChangeListDataSource = helixCoreChangeListDataSource;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, HelixCoreIterativeQuery query) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");

        Data<HelixCoreChangeList> changeList = helixCoreChangeListDataSource.fetchOne(query);

        return new HelixCoreGetChangeListResult(changeList.getPayload());
    }

    @Override
    public HelixCoreIterativeQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, HelixCoreIterativeQuery.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = HelixCoreGetChangeListResult.HelixCoreGetChangeListResultBuilder.class)
    public static class HelixCoreGetChangeListResult implements ControllerIngestionResult {
        @JsonProperty("change_list")
        HelixCoreChangeList changeList;

    }

}
