package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Label.LabelBuilder.class)
public class Label {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("url")
    String url;

    @JsonProperty("description")
    String description;

    @JsonProperty("labelScope")
    String labelScope;

    @JsonProperty("modifiedDate")
    String modifiedDate;

    @JsonProperty("owner")
    IdentityRef owner;
}