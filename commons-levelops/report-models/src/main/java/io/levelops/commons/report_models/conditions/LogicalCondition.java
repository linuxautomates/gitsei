package io.levelops.commons.report_models.conditions;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.report_models.operators.LogicalOperator;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LogicalCondition.LogicalConditionBuilder.class)
public class LogicalCondition implements Condition {
    @JsonProperty("operator")
    LogicalOperator operator;
    @JsonProperty("conditions")
    List<Condition> conditions;


    @Override
    public String explain() {
        List<String> conditionExplains = CollectionUtils.emptyIfNull(conditions).stream().map(Condition::explain).collect(Collectors.toList());
        return "(" + String.join( " " + operator.toString() + " ", conditionExplains) + ")";
    }
}
