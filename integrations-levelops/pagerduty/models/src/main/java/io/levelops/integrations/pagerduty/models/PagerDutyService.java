package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.ingestion.models.IngestionDataType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyService.PagerDutyServiceBuilderImpl.class)
public class PagerDutyService implements PagerDutyEntity {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    // @JsonProperty("summary")
    // @JsonAlias({"summary", "name"})
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Long updatedAt;

    @Override
    public IngestionDataType<PagerDutyEntity, PagerDutyResponse> getIngestionDataType() {
        return PagerDutyIngestionDataType.SERVICE;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class PagerDutyServiceBuilderImpl extends PagerDutyServiceBuilder {
        @JsonProperty("summary")
        public void setSummary(String summary) {
            this.name(summary);
        }

        @JsonProperty("updated_at")
        public void setUpdatedAt(String updatedAt) {
            this.updatedAt(Instant.parse(updatedAt).getEpochSecond());
        }
    }
}