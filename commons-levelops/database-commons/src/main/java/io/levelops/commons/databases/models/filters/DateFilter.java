package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DateFilter.DateFilterBuilder.class)
public class DateFilter {
    @JsonProperty("$lt")
    Long lt;
    @JsonProperty("$gt")
    Long gt;
}