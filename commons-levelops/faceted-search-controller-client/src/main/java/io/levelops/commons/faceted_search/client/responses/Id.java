package io.levelops.commons.faceted_search.client.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Id.IdBuilder.class)
public class Id {
    @JsonProperty("id")
    String id;

    public static Id from(String id) {
        return new Id(id);
    }
}