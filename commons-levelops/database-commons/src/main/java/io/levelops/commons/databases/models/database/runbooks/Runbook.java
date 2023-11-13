package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Runbook.RunbookBuilder.class)
public class Runbook {
    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("enabled")
    Boolean enabled;

    @JsonProperty("previous_id")
    String previousId;

    @JsonProperty("permanent_id")
    String permanentId;

    @JsonProperty("trigger_type")
    TriggerType triggerType;

    @JsonProperty("trigger_template_type")
    String triggerTemplateType;

    /**
     * These variables come from the trigger node's input variables.
     * (used to configure the trigger)
     */
    @JsonProperty("trigger_data")
    Map<String, RunbookVariable> triggerData;

    @JsonProperty("last_run_at")
    Instant lastRunAt; // this for internal purposes (used by scheduled trigger) - for customer-visible data, query the Runs table to get the last run time!

    /**
     * This will be used when we have parameterized runbooks (for example, in "sub" runbooks)
     * to define custom input fields.
     */
    @JsonProperty("input")
    Map<String, KvField> input;

    @JsonProperty("nodes")
    Map<String, RunbookNode> nodes;

    @JsonProperty("ui_data")
    Map<String, Object> uiData;

    @JsonProperty("settings")
    Setting settings;

    @JsonProperty("updated_at")
    Instant updatedAt;

    @JsonProperty("created_at")
    Instant createdAt;

    public static class RunbookBuilder {

        public RunbookBuilder inputVariable(KvField variable) {
            this.input = MapUtils.append(this.input, variable.getKey(), variable);
            return this;
        }

        public RunbookBuilder addNode(RunbookNode node) {
            this.nodes = MapUtils.append(this.nodes, node.getId(), node);
            return this;
        }

        public RunbookBuilder addNodes(RunbookNode... nodes) {
            Arrays.stream(nodes).forEach(this::addNode);
            return this;
        }

        public RunbookBuilder addNodes(Collection<RunbookNode> nodes) {
            nodes.forEach(this::addNode);
            return this;
        }

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Runbook.Setting.SettingBuilder.class)
    public static class Setting {

        @JsonProperty("notifications")
        Notification notifications;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Runbook.Setting.Notification.NotificationBuilder.class)
        public static class Notification {

            @JsonProperty("enabled")
            Boolean enabled;

            @JsonProperty("type")
            String type;

            @JsonProperty("recipients")
            List<String> recipients;
        }
    }
}
