package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "key")
    private String key;

    @JsonProperty(value = "orgIdentifier")
    private String orgIdentifier;

    @JsonProperty(value = "owner_id")
    private String ownerId;
    
    @JsonProperty("integration_ids")
    Set<Integer> integrationIds;

    @JsonProperty("integrations")
    Set<Integration> integrations;

    @JsonProperty(value = "updated_at")
    private Long updatedAt;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    @JsonProperty("bootstrapped")
    private Boolean bootstrapped;

    @JsonProperty("immutable")
    private Boolean immutable;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("demo")
    private Boolean demo;
}
