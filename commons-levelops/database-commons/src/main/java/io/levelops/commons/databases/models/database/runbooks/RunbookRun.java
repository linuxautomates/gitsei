package io.levelops.commons.databases.models.database.runbooks;

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
@JsonDeserialize(builder = RunbookRun.RunbookRunBuilder.class)
public class RunbookRun {

    @JsonProperty("id")
    String id;
    @JsonProperty("runbook_id")
    String runbookId;
    @JsonProperty("permanent_id")
    String permanentId;
    @JsonProperty("trigger_type")
    String triggerType; // which trigger actually started this run (a triggered runbook could also be started manually for example)
    @JsonProperty("args")
    Map<String, RunbookVariable> args; // input arguments given to this run
    @JsonProperty("state")
    RunbookRunState state;
    @JsonProperty("has_warnings")
    Boolean hasWarnings; // for indexing, if state = success, but there were some warnings to report
    @JsonProperty("result")
    RunbookRunResult result; // to store result of evaluation (like errors)
    @JsonProperty("state_changed_at")
    Instant stateChangedAt;
    @JsonProperty("created_at")
    Instant createdAt;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookRunResult.RunbookRunResultBuilder.class)
    public static class RunbookRunResult {
        @JsonProperty("errors")
        List<RunbookError> errors;

        @JsonProperty("data")
        Map<String, Object> data;

        @JsonProperty("output")
        Map<String, RunbookVariable> output;
    }


    public static class RunbookRunBuilder {

        public RunbookRunBuilder argVariable(RunbookVariable variable) {
            this.args = MapUtils.append(this.args, variable.getName(), variable);
            return this;
        }

    }

}
