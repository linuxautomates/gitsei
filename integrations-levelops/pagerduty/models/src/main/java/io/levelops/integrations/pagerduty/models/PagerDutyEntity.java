package io.levelops.integrations.pagerduty.models;

import io.levelops.commons.models.IngestionDataEntity;
import io.levelops.ingestion.models.SelfIdentifiableIngestionDataType;

public interface PagerDutyEntity extends IngestionDataEntity, SelfIdentifiableIngestionDataType<PagerDutyEntity, PagerDutyResponse> {

}