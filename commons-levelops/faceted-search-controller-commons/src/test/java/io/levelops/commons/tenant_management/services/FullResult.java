package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = FullResult.FullResultBuilder.class)
public class FullResult {
    @JsonProperty("aggregations")
    Map<String, CategoryResult> aggregations;
}
