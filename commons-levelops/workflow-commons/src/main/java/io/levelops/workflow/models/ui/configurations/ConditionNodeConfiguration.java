package io.levelops.workflow.models.ui.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.workflow.models.WorkflowPolicy;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConditionNodeConfiguration.ConditionNodeConfigurationBuilder.class)
public class ConditionNodeConfiguration implements NodeConfiguration {

    @JsonProperty("integration_type")
    String integrationType;

    @JsonProperty("integration_ids")
    List<String> integrationIds;

    @JsonProperty("signature_condition")
    Rule signatureCondition;

    @JsonProperty("aggregate_condition")
    Rule aggregateCondition;

    @JsonProperty("condition_type")
    WorkflowPolicy.ConditionType conditionType;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Rule.RuleBuilder.class)
    public static class Rule {

        @JsonProperty("type")
        String type; // always condition for parent node

        @JsonProperty("name")
        String name;  // always or for parent node

        @JsonProperty("id")
        String id;

        @JsonProperty("signature_id")
        String signatureId;

        @JsonProperty("payload")
        Map<String, Object> payload; // polymorphic based on signature

        @JsonProperty("children")
        List<Rule> children; // only type 'condition' can have children
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DefaultSignaturePayload.DefaultSignaturePayloadBuilder.class)
    public static class DefaultSignaturePayload {

        @JsonProperty("operator")
        String operator; // e.g. "=="

        @JsonProperty("value")
        String value; // e.g. "true"
    }
}
