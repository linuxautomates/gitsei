package io.levelops.integrations.storage.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.sinks.SinkIngestionResult;
import io.levelops.integrations.gcs.models.GcsDataResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage result model that points to the pushed data.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
// NB: cannot use @Value + @JsonDeserialize with @JsonUnwrapped! https://github.com/springfox/springfox/issues/2358
public class StorageResult implements SinkIngestionResult, ControllerIngestionResult, Serializable {

    @JsonProperty("_metadata")
    StorageMetadata storageMetadata;

    @Builder.Default
    @JsonProperty("destination")
    String destination = "gcs";

    @JsonProperty("prefix_uri")
    String prefixUri;

    // region multi-page ------------
    @JsonProperty("records")
    List<GcsDataResult> records;

    @JsonProperty("failures")
    List<IngestionFailure> ingestionFailures;

    @JsonProperty("count")
    public int getCount() {
        return CollectionUtils.size(records);
    }
    //endregion


    public void setIngestionFailures(List<IngestionFailure> ingestionFailures) {
        this.ingestionFailures = ingestionFailures;
    }

    public static class StorageResultBuilder {

        public StorageResultBuilder record(GcsDataResult record) {
            if (this.records == null) {
                this.records = new ArrayList<>();
            }
            this.records.add(record);
            return this;
        }
    }
}
