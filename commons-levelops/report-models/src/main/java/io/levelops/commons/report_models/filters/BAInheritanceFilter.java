package io.levelops.commons.report_models.filters;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.report_models.conditions.LogicalCondition;
import io.levelops.commons.report_models.inheritance.WIInheritance;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BAInheritanceFilter.BAInheritanceFilterBuilder.class)
public class BAInheritanceFilter {
    @JsonProperty("condition")
    LogicalCondition condition;
    @JsonProperty("inheritance")
    WIInheritance inheritance;

    public String explain() {
        String inheritanceExplain = inheritance.getExplainText();
        if (StringUtils.isNotEmpty(inheritanceExplain)) {
            return inheritanceExplain + " " + condition.explain();
        } else {
            return condition.explain();
        }
    }
}
