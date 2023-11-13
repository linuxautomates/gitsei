package io.levelops.commons.functional;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class IngestionFailure {
    @JsonProperty("message")
    String message;

    @JsonProperty("url")
    String url;

    @JsonProperty("severity")
    Severity severity;

    public enum Severity {
        WARNING,
        ERROR;
    }
}
