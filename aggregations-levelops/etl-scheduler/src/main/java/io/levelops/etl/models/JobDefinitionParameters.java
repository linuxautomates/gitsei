package io.levelops.etl.models;

import io.levelops.commons.etl.models.JobPriority;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder(toBuilder = true)
public class JobDefinitionParameters {
    @NonNull
    private final JobPriority jobPriority;
    @NonNull
    private final Integer attemptMax;
    @NonNull
    private final Integer retryWaitTimeInMinutes;
    @NonNull
    private final Long timeoutInMinutes;
    @NonNull
    private final Integer frequencyInMinutes;
    @NonNull
    private final Integer fullFrequencyInMinutes;
}
