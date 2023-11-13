package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.ZendeskAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.databases.services.ZendeskTicketService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@Log4j2
@Service
@SuppressWarnings("unused")
public class ZendeskAggregationsController implements AggregationsController<AppAggMessage> {
    private static final String AGGREGATION_VERSION = "V0.1";

    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final ZendeskFieldService fieldService;
    private final ZendeskTicketService zendeskTicketService;
    private final ZendeskAggHelper helper;
    private final String subscriptionName;

    @Autowired
    ZendeskAggregationsController(Storage storage,
                                  @Value("${ZENDESK_AGG_SUB:dev-zendesk-sub}") String subscriptionName,
                                  AggregationsDatabaseService aggregationsDatabaseService,
                                  IntegrationService integrationService, ControlPlaneService controlPlaneService,
                                  ZendeskFieldService fieldService, ZendeskTicketService zendeskTicketService,
                                  ZendeskAggHelper helper) {
        this.subscriptionName = subscriptionName;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.fieldService = fieldService;
        this.zendeskTicketService = zendeskTicketService;
        this.helper = helper;
    }

    @Override
    @Async("zendeskTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("doTask: received message: {}", message.getMessageId());
            Integration it = integrationService.get(
                            message.getCustomer(),
                            message.getIntegrationId())
                    .orElse(null);
            if (it == null ||
                    IntegrationType.ZENDESK != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            final String integrationId = message.getIntegrationId();
            Integration integration = integrationService.get(message.getCustomer(), integrationId)
                    .orElse(null);
            if (integration == null ||
                    IntegrationType.ZENDESK != IntegrationType.fromString(integration.getApplication())) {
                return;
            }
            MultipleTriggerResults multipleTriggerResults = controlPlaneService.getAllTriggerResults(
                    IntegrationKey.builder().tenantId(message.getCustomer()).integrationId(integration.getId()).build(),
                    false,
                    false,
                    true);
            IntegrationConfig config = integrationService.listConfigs(message.getCustomer(), List.of(it.getId()),
                    0, 1).getRecords().stream().findFirst().orElse(null);
            List<DbZendeskField> customFields = fieldService.listByFilter(message.getCustomer(),
                    List.of(message.getIntegrationId()),
                    null,
                    null,
                    null,
                    null,
                    0,
                    1000000).getRecords();
            final Date currentTime = new Date();
            if (!helper.setupZendeskTickets(
                    message.getCustomer(),
                    integrationId,
                    (config == null || config.getConfig() == null) ?
                            null : config.getConfig().get("agg_custom_fields"),
                    multipleTriggerResults,
                    customFields,
                    currentTime)) {
                log.warn("doTask: Failed to setup zendesk tickets aggregation");
            }
            log.info("doTask: task completed for {}", message.getMessageId());
            final int numRows = zendeskTicketService.cleanUpOldData(message.getCustomer(), currentTime,
                    86400 * 91L);
            log.info("doTask: cleaned up older data, dropped {} rows", numRows);
        } catch (IngestionServiceException | SQLException e) {
            log.error("doTask: error performing aggregations: " + e.getMessage(), e);
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.ZENDESK;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }
}
