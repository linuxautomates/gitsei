package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AgentPoolQueue.AgentPoolQueueBuilder.class)
public class AgentPoolQueue {

    @JsonProperty("id")
    int id;

    @JsonProperty("name")
    String name;

    @JsonProperty("pool")
    AgentPoolReference pool;

    @JsonProperty("url")
    String url;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AgentPoolReference.AgentPoolReferenceBuilder.class)
    public static class AgentPoolReference {

        @JsonProperty("id")
        int id;

        @JsonProperty("isHosted")
        boolean isHosted;

        @JsonProperty("name")
        String name;
    }
}
