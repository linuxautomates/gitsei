package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.commons.models.ExceptionPrintout;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
@Log4j2
public class Job {

    //NB: this is the Agent's job model
    // TODO improve models? (interfaces)

    @JsonProperty("id")
    private String id;

    @JsonProperty("controller_id")
    private String controllerId;

    @JsonProperty("controller_name")
    private String controllerName;

    @JsonProperty("agent_id")
    private String agentId;

    @JsonProperty("query")
    private Map<String, Object> query;

    @JsonProperty("done")
    private boolean done;

    @JsonProperty("cancelled")
    private boolean cancelled;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("done_at")
    private Date doneAt;

    @JsonProperty("exception")
    private ExceptionPrintout exception;

    @JsonProperty("result")
    private Map<String, Object> result;

    @JsonProperty("intermediate_state")
    private Map<String, Object> intermediateState;

    @JsonProperty("failures")
    List<IngestionFailure> ingestionFailures;

    @JsonIgnore
    public boolean isSuccessful() {
        return done && !cancelled && exception == null && result != null;
    }
}
