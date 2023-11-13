package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNodeState;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.dates.DateUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookRunningNodeDTO.RunbookRunningNodeDTOBuilder.class)
public class RunbookRunningNodeDTO {
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
    RunbookRunningNode.RunbookRunningNodeResult result; // to store result of evaluation (like errors)
    @JsonProperty("state")
    RunbookRunningNodeState state;
    @JsonProperty("state_changed_at")
    Date stateChangedAt;
    @JsonProperty("created_at")
    Date createdAt;

    private static final Set<String> EXPOSED_RUNBOOK_DATA_FIELDS = Set.of("script_data", "script_log", "error");

    public static RunbookRunningNodeDTO convert(RunbookRunningNode runningNode) {
        return RunbookRunningNodeDTO.builder()
                .id(runningNode.getId())
                .runId(runningNode.getRunId())
                .nodeId(runningNode.getNodeId())
                .triggeredBy(runningNode.getTriggeredBy())
                .output(sanitizeOutput(runningNode.getOutput()))
                .data(sanitizeData(runningNode.getData()))
                .hasWarnings(runningNode.getHasWarnings())
                .result(runningNode.getResult())
                .state(runningNode.getState())
                .stateChangedAt(DateUtils.toDate(runningNode.getStateChangedAt()))
                .createdAt(DateUtils.toDate(runningNode.getCreatedAt()))
                .build();
    }

    private static Map<String, Object> sanitizeData(Map<String, Object> data) {
        return MapUtils.emptyIfNull(data).entrySet().stream()
                .filter(kv -> EXPOSED_RUNBOOK_DATA_FIELDS.contains(StringUtils.lowerCase(kv.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    private static Map<String, RunbookVariable> sanitizeOutput(Map<String, RunbookVariable> output) {
        return MapUtils.emptyIfNull(output).values().stream()
                .map(variable->RunbookVariable.builder()
                        .name(variable.getName())
                        .value(variable.getValue())
                        .valueType(variable.getValueType())
                        .build())
                .collect(Collectors.toMap(RunbookVariable::getName, v->v, (a, b) -> b));
    }
}
