package io.levelops.integrations.pagerduty.models;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;

public interface PagerDutyDataQuery extends DataQuery{
    IntegrationKey getIntegrationKey();
}