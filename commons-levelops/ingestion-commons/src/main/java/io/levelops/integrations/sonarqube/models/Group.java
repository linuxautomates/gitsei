package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;


@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = Group.GroupBuilder.class)
public class Group {

    @JsonProperty("uuid")
    String uuid;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("membersCount")
    long membersCount;

    @JsonProperty("default")
    Boolean defaults;

}