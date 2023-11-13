package io.levelops.commons.report_models.conditions;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.report_models.ESField;
import io.levelops.commons.report_models.operators.ComparisionOperator;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder (toBuilder = true)
@JsonDeserialize(builder = WIBaseCondition.WIBaseConditionBuilder.class)
public class WIBaseCondition implements Condition {
    @JsonProperty("field")
    ESField field;
    @JsonProperty("custom_field_name")
    String customFieldName;

    @JsonProperty("comparision")
    ComparisionOperator comparision;

    //Single values -- used for =, !=
    @JsonProperty("int_value")
    Integer intValue;
    @JsonProperty("long_value")
    Long longValue;
    @JsonProperty("str_value")
    String strValue;
    @JsonProperty("bool_value")
    Boolean boolValue;

    //Collection values -- used for in, nin
    @JsonProperty("values")
    List<String> values;

    public String explain() {
        StringBuffer sb = new StringBuffer();
        String fieldName = null;
        if (field == ESField.w_custom_field) {
            fieldName = field.readableString() + "." + customFieldName;
        } else {
            fieldName = field.readableString();
        }

        String comparisionCondition = comparision.getReadableFormat();

        String valueStr = null;
        if (comparision.isMultiValue()) {
            valueStr = "(" + String.join(",", values) + ")";
        } else {
            if(intValue != null) {
                valueStr = intValue.toString();
            } else if (longValue != null) {
                valueStr = longValue.toString();
            } else if (strValue != null) {
                valueStr = strValue.toString();
            } else if (boolValue != null) {
                valueStr = boolValue.toString();
            }
        }

        String text = fieldName + " " + comparisionCondition + " " + valueStr;
        if (comparision.isNotCondition()) {
            text = "NOT ( " + text + " )";
        }
        return text;
    }
}
