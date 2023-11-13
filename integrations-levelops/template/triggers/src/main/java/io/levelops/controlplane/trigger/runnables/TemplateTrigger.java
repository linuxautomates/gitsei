package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy.DefaultCursor;
import io.levelops.ingestion.integrations.template.models.TemplateScanQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class TemplateTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "TemplateIterativeScanController";

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    @Autowired
    public TemplateTrigger(@Value("${TEMPLATE_FULL_SCAN_FREQ_IN_MIN:7}") Integer fullScanFrequencyInMinutes,
                           ObjectMapper objectMapper,
                           TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(fullScanFrequencyInMinutes));
        this.triggerActionService = triggerActionService;
    }

    @Override
    public String getTriggerType() {
        return "template";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        TemplateTriggerMetadata metadata = parseMetadata(trigger);
        Cursor cursor = cursorStrategy.getNextCursor(metadata.getLastIterativeScan(), metadata.getLastFullScan(), trigger.getCreatedAt(), new Date());

        log.debug("onboard?={} ({} - {})", !cursor.isPartial(), cursor.getFrom(), cursor.getTo());

        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder().tags(cursor.getTags())
                .controllerName(CONTROLLER_NAME)
                .query(TemplateScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom())
                        .to(cursor.getTo())
                        .build())
                .build());

        TemplateTriggerMetadata updatedMetadata = TemplateTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : cursor.getTo())
                .lastIterativeScan(cursor.getTo())
                .build();

        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    private TemplateTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return TemplateTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), TemplateTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TemplateTriggerMetadata.TemplateTriggerMetadataBuilder.class)
    public static class TemplateTriggerMetadata {
        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
