package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean defining the response from a search query for any entity containing a "id" field
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IdSearchResult.IdSearchResultBuilder.class)
public class IdSearchResult {

    @JsonProperty
    List<EntityId> results;

    @JsonProperty("next_page")
    String nextPage;

    @JsonProperty("previous_page")
    String previousPage;

    @JsonProperty
    Integer count;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EntityId.EntityIdBuilder.class)
    public static class EntityId {

        @JsonProperty
        Long id;
    }
}
