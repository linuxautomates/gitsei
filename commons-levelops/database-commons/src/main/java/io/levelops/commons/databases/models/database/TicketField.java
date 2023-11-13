package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
public class TicketField {

    @JsonProperty("id")
    private Long id;

    @JsonUnwrapped
    KvField field;

    @JsonProperty("ticket_template_id")
    private String ticketTemplateId;

    @JsonProperty("deleted")
    private Boolean deleted;
}
