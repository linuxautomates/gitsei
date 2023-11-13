package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookRunningNode.RunbookRunningNodeBuilder.class)
public class RunbookRunningNode {

    @JsonProperty("id")
    String id;
    @JsonProperty("run_id")
    String runId;
    @JsonProperty("node_id")
    String nodeId;
    @JsonProperty("triggered_by")
    Map<String, String> triggeredBy; // nodeId -> runningNodeId
    @JsonProperty("output")
    Map<String, RunbookVariable> output; // output variables emitted by the node handler during evaluation
    @JsonProperty("data")
    Map<String, Object> data; // for internal data (used by node handlers and during node evaluation)
    @JsonProperty("has_warnings")
    Boolean hasWarnings; // for indexing, if state = success, but there were some warnings to report
    @JsonProperty("result")
    RunbookRunningNodeResult result; // to store result of evaluation (like errors)
    @JsonProperty("state")
    RunbookRunningNodeState state;
    @JsonProperty("state_changed_at")
    Instant stateChangedAt;
    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookRunningNodeResult.RunbookRunningNodeResultBuilder.class)
    public static class RunbookRunningNodeResult {
        @JsonProperty("errors")
        List<RunbookError> errors;
    }

    public static class RunbookRunningNodeBuilder {

        public RunbookRunningNodeBuilder outputVariable(RunbookVariable variable) {
            this.output = MapUtils.append(this.output, variable.getName(), variable);
            return this;
        }

    }
}
