package io.levelops.commons.report_models.filters;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.report_models.operators.LogicalOperator;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.BooleanUtils;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BACategoryFilter.BACategoryFilterBuilder.class)
public class BACategoryFilter {
    @JsonProperty("inheritance_filter")
    BAInheritanceFilter inheritanceFilter;
    @JsonProperty("non_inheritance_filter")
    BANonInheritanceFilter nonInheritanceFilter;

    /*
    If exclude_non_inheritance is true, then inheritance_filter AND non_inheritance_filter
    If exclude_non_inheritance is false or null, then inheritance_filter OR non_inheritance_filter
     */
    @JsonProperty("exclude_non_inheritance")
    Boolean excludeNonInheritance;

    public String explain() {
        String condition = (BooleanUtils.isTrue(excludeNonInheritance)) ? "and" : "or";
        return inheritanceFilter.explain() + " " + condition + " " + nonInheritanceFilter.explain();
    }

}
