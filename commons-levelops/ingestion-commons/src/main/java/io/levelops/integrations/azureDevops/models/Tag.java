package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Tag.TagBuilder.class)
public class Tag {

    @JsonProperty("name")
    String name;

    @JsonProperty("objectId")
    String objectId;

    @JsonProperty("creator")
    IdentityRef creator;

    @JsonProperty("url")
    String url;

    @JsonProperty("taggedBy")
    GitUserDate taggedBy;   //enriched

    @JsonProperty("taggedObject")
    GitTaggedObject taggedObject; //enriched

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Tag.GitUserDate.GitUserDateBuilder.class)
    public static class GitUserDate {

        @JsonProperty("name")
        String name;

        @JsonProperty("email")
        String email;

        @JsonProperty("date")
        String date;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Tag.GitTaggedObject.GitTaggedObjectBuilder.class)
    public static class GitTaggedObject {

        @JsonProperty("objectId")
        String objectId;

        @JsonProperty("objectType")
        String objectType;
    }
}