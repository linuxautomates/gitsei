package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Fail.FailBuilder.class)
public class Fail {

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("branch")
    String branch;



}