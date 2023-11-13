package io.levelops.integrations.template.models;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;

public interface TemplateDataQuery extends DataQuery{
    IntegrationKey getIntegrationKey();
    String getOffsetLink();
}