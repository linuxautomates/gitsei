package io.levelops.ingestion.models.controlplane;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum JobStatus {
    UNASSIGNED, // Job request has been submitted but is not yet scheduled for assignment to an agent
    SCHEDULED, //  Job request has been scheduled and may be accepted by compatible agents
    ACCEPTED, //   Job request has been accepted by an agent (this locks the request to the agent)
    PENDING, //    Job is currently running
    SUCCESS, //    Job is done and successful
    FAILURE, //    Job is done and failed
    CANCELED, //   Job has been request to be canceled
    ABORTED, //    Job was canceled and got successfuly aborted
    INVALID; //    Job could not be executed due to invalid request

    public static final String SWAGGER_VALUES = "(all),unassigned,scheduled,accepted,pending,success,failure,canceled,aborted,invalid";

    @Nullable
    @JsonCreator
    public static JobStatus fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(JobStatus.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
