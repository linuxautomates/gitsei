package io.levelops.etl.jobs.harnessng;

import io.levelops.aggregations_shared.helpers.harnessng.HarnessNGAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Log4j2
@Service
public class HarnessNGPipelineStage extends BaseIngestionResultProcessingStage<HarnessNGPipelineExecution, HarnessNGJobState> {
    private final HarnessNGAggHelperService helper;
    private final EventsClient eventsClient;

    public HarnessNGPipelineStage(HarnessNGAggHelperService helper, EventsClient eventsClient) {
        this.helper = helper;
        this.eventsClient = eventsClient;
    }

    @Override
    public String getName() {
        return "HarnessNG Pipeline Stage";
    }

    @Override
    public void preStage(JobContext context, HarnessNGJobState jobState) throws SQLException {
    }

    @Override
    public void process(JobContext context, HarnessNGJobState jobState, String ingestionJobId, HarnessNGPipelineExecution entity) throws SQLException {
        helper.processPipeline(context.getTenantId(), context.getIntegrationId(), entity);
    }

    @Override
    public void postStage(JobContext context, HarnessNGJobState jobState) {
        String customer = context.getTenantId();
        IntegrationKey integrationKey = IntegrationKey.builder().integrationId(context.getIntegrationId()).tenantId(customer).build();
        long startTime = Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli();

        if(context.getIsFull())
            startTime = helper.getOldestJobRunStartTime(context.getTenantId(), context.getIntegrationId());

        try {
            eventsClient.emitEvent(customer, EventType.HARNESSNG_NEW_AGGREGATION, Map.of("integration_key", integrationKey, "start_time", startTime));
        } catch (EventsClientException e) {
            log.error("Error sending event for tenant={}, eventType={}", customer, EventType.HARNESSNG_NEW_AGGREGATION, e);
        }
    }

    @Override
    public String getDataTypeName() {
        return "pipeline";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }
}
