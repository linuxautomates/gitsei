package io.levelops.commons.functional;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class IngestionResult<T> {
    @JsonProperty("data")
    List<T> data;

    @JsonProperty("failures")
    List<IngestionFailure> ingestionFailures;
}
