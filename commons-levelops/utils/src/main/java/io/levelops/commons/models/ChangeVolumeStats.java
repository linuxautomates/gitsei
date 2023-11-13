package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonDeserialize(builder = ChangeVolumeStats.ChangeVolumeStatsBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeVolumeStats {

    @JsonProperty("fileName")
    String fileName;

    @JsonProperty("additions")
    Integer additions;

    @JsonProperty("deletions")
    Integer deletions;

    @JsonProperty("changes")
    Integer changes;
}
