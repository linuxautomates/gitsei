package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Rule.RuleBuilder.class)
public class Rule {
 @JsonProperty("key")
 String key;

 @JsonProperty("name")
 String name;

 @JsonProperty("status")
 String status;

 @JsonProperty("lang")
 String lang;

 @JsonProperty("langName")
 String langName;
}
