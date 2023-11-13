package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketTemplate {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "default_fields")
    private Map<String, Boolean> defaultFields;

    @Singular
    @JsonProperty("ticket_fields")
    private List<TicketField> ticketFields;

    @Default
    @JsonProperty("notify_by")
    private Map<EventType, List<String>> notifyBy = new HashMap<>();

    @JsonProperty(value = "message_template_ids")
    private List<String> messageTemplateIds;

    @Singular
    @JsonProperty("questionnaire_templates")
    private List<TicketTemplateQuestionnaireTemplateMapping> mappings;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("default")
    private Boolean isDefault = false; // this is currently not stored in the ticket template table

    @JsonProperty(value = "updated_at")
    private Long updatedAt;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    @JsonGetter("notify_by")
    public Map<String, List<String>> getNotifyByString() throws JsonProcessingException {
        if (this.notifyBy == null || this.notifyBy.size() == 0) {
            return Map.of();
        }
        return this.notifyBy.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue()));
    }
}
