package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraPriorityScheme.JiraPrioritySchemeBuilder.class)
public class JiraPriorityScheme {

    @JsonProperty("expand")
    String expand;

    @JsonProperty("self")
    String self;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("defaultOptionId")
    String defaultOptionId;

    @JsonProperty("optionIds")
    List<String> optionIds;

    @JsonProperty("defaultScheme")
    Boolean scheme;

    //Enriched
    @JsonProperty("priorities")
    List<JiraPriority> priorities;

}
