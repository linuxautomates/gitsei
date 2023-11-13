package io.levelops.commons.report_models.conditions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({ @JsonSubTypes.Type(WIBaseCondition.class), @JsonSubTypes.Type(LogicalCondition.class) })
public interface Condition {
    String explain();
}
