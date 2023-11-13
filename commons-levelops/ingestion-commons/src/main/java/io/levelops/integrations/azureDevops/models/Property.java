package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Property.PropertyBuilder.class)
public class Property {

    @JsonProperty("count")
    int count;

    @JsonProperty("item")
    String item;

    @JsonProperty("keys")
    List<String> keys;

    @JsonProperty("values")
    List<String> values;

    @JsonProperty("CodeReviewThreadType")
    PropertyValue codeReviewThreadType;

    @JsonProperty("CodeReviewVoteResult")
    PropertyValue codeReviewVoteResult;

    @JsonProperty("CodeReviewVotedByInitiatorIdentity")
    PropertyValue codeReviewVotedByInitiatorIdentity;

    @JsonProperty("CodeReviewVotedByIdentity")
    PropertyValue codeReviewVotedByIdentity;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PropertyValue.PropertyValueBuilder.class)
    public static class PropertyValue {

        @JsonProperty("$type")
        String type;

        @JsonProperty("$value")
        String value;
    }

}
