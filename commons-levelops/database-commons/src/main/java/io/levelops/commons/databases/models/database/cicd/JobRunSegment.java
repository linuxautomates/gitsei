package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Value
@NonFinal
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = JobRunSegment.JobRunSegmentBuilder.class)
public class JobRunSegment {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("type")
    private SegmentType type;
    @JsonProperty("job_number")
    private String jobNumber;
    @NonNull
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @NonNull
    @JsonProperty("result")
    private String result;
    @NonNull
    @JsonProperty("duration")
    private Integer duration;
    @JsonProperty("logs")
    private String logs;
    @JsonProperty("start_time")
    private Instant startTime;
    @JsonProperty("url")
    private String url;
    @NonNull
    @JsonProperty("full_path")
    private Set<PathSegment> fullPath;

    @JsonPOJOBuilder(withPrefix = "")
    static final class JobRunSegmentBuilderImpl extends JobRunSegment.JobRunSegmentBuilder<JobRunSegment, JobRunSegment.JobRunSegmentBuilderImpl> {

    }
}
