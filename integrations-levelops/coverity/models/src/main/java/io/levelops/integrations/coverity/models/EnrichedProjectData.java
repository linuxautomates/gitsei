package io.levelops.integrations.coverity.models;

import com.coverity.ws.v9.MergedDefectDataObj;
import com.coverity.ws.v9.SnapshotInfoDataObj;
import com.coverity.ws.v9.StreamDataObj;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EnrichedProjectData.EnrichedProjectDataBuilder.class)
public class EnrichedProjectData {

    @JsonProperty("stream")
    StreamDataObj stream;

    @JsonProperty("snapshot")
    SnapshotInfoDataObj snapshot;

    @JsonProperty("defects")
    List<MergedDefectDataObj> defects;
}
