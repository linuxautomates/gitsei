package io.levelops.commons.etl.models.job_progress;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = FileProgressDetail.FileProgressDetailBuilder.class)
public class FileProgressDetail {
    @JsonProperty("entityProgress")
    EntityProgressDetail entityProgressDetail;

    @JsonProperty("duration_in_milliseconds")
    Long durationMilliSeconds;

    // Arbitrary failures that a job can add to bubble up failure information to the top
    // TODO: expose this in the job context, this isn't used right now
    @JsonProperty("failures")
    List<String> failures;
}
