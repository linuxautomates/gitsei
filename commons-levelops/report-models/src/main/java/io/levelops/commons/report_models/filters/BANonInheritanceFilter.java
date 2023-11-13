package io.levelops.commons.report_models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.report_models.conditions.LogicalCondition;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BANonInheritanceFilter.BANonInheritanceFilterBuilder.class)
public class BANonInheritanceFilter {
    @JsonProperty("condition")
    LogicalCondition condition;

    public String explain() {
        return condition.explain();
    }
}
