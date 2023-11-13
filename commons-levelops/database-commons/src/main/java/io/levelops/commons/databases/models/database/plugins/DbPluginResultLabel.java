package io.levelops.commons.databases.models.database.plugins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbPluginResultLabel.DbPluginResultLabelBuilder.class)
public class DbPluginResultLabel {

    @JsonProperty("result_id")
    String resultId; // UUID

    @JsonProperty("key")
    String key;

    @JsonProperty("value")
    String value;

}
