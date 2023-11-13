package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.ExceptionPrintout;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookError.RunbookErrorBuilder.class)
public class RunbookError {

    // type of error - this should end up being an enum
    @JsonProperty("type")
    String type;

    // keep this consistent - might change to be data driven
    @JsonProperty("description")
    String description;

    @JsonProperty("details")
    Map<String, Object> details;

    @JsonProperty("exception_printout")
    ExceptionPrintout exceptionPrintout;

}
