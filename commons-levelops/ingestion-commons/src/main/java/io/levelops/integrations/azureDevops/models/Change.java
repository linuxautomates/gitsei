package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Change.ChangeBuilder.class)
public class Change {

    @JsonProperty("item")
    Item item;

    @JsonProperty("changeType")
    String changeType;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Item.ItemBuilder.class)
    public static class Item {
        @JsonProperty("objectId")
        String objectId;

        @JsonProperty("gitObjectType")
        String gitObjectType;

        @JsonProperty("commitId")
        String commitId;

        @JsonProperty("path")
        String path;

        @JsonProperty("url")
        String url;
    }
}
