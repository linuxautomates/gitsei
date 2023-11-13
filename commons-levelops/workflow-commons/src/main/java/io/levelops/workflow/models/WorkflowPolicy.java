package io.levelops.workflow.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.workflow.models.ui.configurations.ConditionNodeConfiguration;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkflowPolicy.WorkflowPolicyBuilder.class)
public class WorkflowPolicy {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("type")
    WorkflowPolicyType type; // policy or status_check

    @JsonProperty("workflow_id")
    String workflowId;

    // region condition fields
    @JsonProperty("integration_type")
    String integrationType; // condition only

    @JsonProperty("integration_ids")
    List<String> integrationIds; // condition only

    @JsonProperty("condition")
    Condition condition; // condition only
    // endregion

    // region check status fields
    @JsonProperty("parent_id")
    String parentId; // status check only

    @JsonProperty("action_id")
    String actionId; // status check only

    @JsonProperty("exit_status")
    String exitStatus; // status check only

    @JsonProperty("cron")
    String cron; // status check only
    // endregion

    @JsonProperty("last_run")
    Integer lastRun;

    @JsonProperty("actions")
    List<Action> actions;

    public enum WorkflowPolicyType {
        POLICY,
        CHECK_STATUS;

        @JsonCreator
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        @Nullable
        public WorkflowPolicyType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(WorkflowPolicyType.class, value);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Condition.ConditionBuilder.class)
    public static class Condition {
        @JsonProperty("signature_condition")
        ConditionNodeConfiguration.Rule signatureCondition;

        @JsonProperty("aggregate_condition")
        ConditionNodeConfiguration.Rule aggregateCondition;

        @JsonProperty("condition_type")
        ConditionType conditionType;
    }

    public enum ConditionType {
        SIGNATURE,
        AGGREGATE;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        @Nullable
        public static ConditionType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(ConditionType.class, value);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Action.ActionBuilder.class)
    public static class Action {
        @JsonProperty("id")
        String id;

        @JsonProperty("type")
        String type;

        @JsonProperty("on_status")
        String onStatus; // pass, fail, ...

        @JsonProperty("name")
        String name;

        @JsonProperty("payload")
        Map<String, Object> payload;

        // temporary, used for parsing purposes
        @JsonIgnore
        String nodeId;

        @JsonIgnore
        public String getNodeId() {
            return nodeId;
        }
    }

    public static class WorkflowPolicyBuilder {

        // needed for adding right action id to new actions
        public int getActionsSize() {
            return CollectionUtils.size(actions);
        }

        // not using @Singular cause it's conflicting with Jackson deserialization
        public WorkflowPolicyBuilder action(Action action) {
            if (actions == null) {
                actions = new ArrayList<>();
            }
            actions.add(action);
            return this;
        }
    }
}
