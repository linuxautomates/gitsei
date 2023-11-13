package io.levelops.commons.etl.models.job_progress;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = StageProgressDetail.StageProgressDetailBuilder.class)
public class StageProgressDetail {
    // The key to this map is the index of the file in the payload of the dbJobInstance
    @JsonProperty("file_progress_map")
    Map<Integer, FileProgressDetail> fileProgressMap;
}
