package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ListTicketFieldsResponse.ListTicketFieldsResponseBuilder.class)
public class ListTicketFieldsResponse {
    @JsonProperty("ticket_fields")
    List<Field> fields;

    @JsonProperty
    int count;
}
