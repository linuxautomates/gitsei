package io.levelops.commons.etl.models.job_progress;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EntityProgressDetail.EntityProgressDetailBuilder.class)
public class EntityProgressDetail {
    @JsonProperty("total_entities")
    public Integer totalEntities;

    @JsonProperty("successful")
    public Integer successful;

    @JsonProperty("failed")
    public Integer failed;
}
