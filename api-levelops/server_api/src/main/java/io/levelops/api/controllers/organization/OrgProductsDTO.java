package io.levelops.api.controllers.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgProductsDTO.OrgProductsDTOBuilderImpl.class)
public class  OrgProductsDTO {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("integrations")
    Map<String,Set<Integ>> integrations;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Integ.IntegBuilder.class)
    public static class Integ{
        @JsonProperty("id")
        int id;
        @JsonProperty("name")
        String name;
        @JsonProperty("filters")
        Map<String, Object> filters;
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    static final class OrgProductsDTOBuilderImpl extends OrgProductsDTOBuilder {

    }
}
