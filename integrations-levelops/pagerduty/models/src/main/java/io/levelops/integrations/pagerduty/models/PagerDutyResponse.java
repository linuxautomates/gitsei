package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.ingestion.models.SelfIdentifiableIngestionDataType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public abstract class PagerDutyResponse implements SelfIdentifiableIngestionDataType<PagerDutyEntity, PagerDutyResponse>{
    @JsonProperty("limit")
    protected int limit;
    @JsonProperty("offset")
    protected int offset;
    @JsonProperty("more")
    protected boolean more;
    @JsonProperty("total")
    protected int total;
}