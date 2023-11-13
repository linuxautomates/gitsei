package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Item.ItemBuilder.class)
public class Item {
    @JsonProperty("version")
    String version;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("hashValue")
    String hashValue;

    @JsonProperty("path")
    String path;

    @JsonProperty("url")
    String url;

}
