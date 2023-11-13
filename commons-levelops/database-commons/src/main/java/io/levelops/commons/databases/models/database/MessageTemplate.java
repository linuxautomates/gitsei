package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder.Default;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageTemplate {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "type")
    private TemplateType type;

    @JsonProperty(value = "bot_name")
    private String botName;

    @JsonProperty(value = "email_subject")
    private String emailSubject;

    @JsonProperty(value = "message")
    private String message;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    @Default
    @JsonProperty("default")
    private boolean defaultTemplate = false;

    @Default
    @JsonProperty("system")
    private boolean system = false;

    @Default
    @JsonProperty("event_type")
    private EventType eventType = EventType.ALL;

    @JsonGetter("event_type")
    public String getEventTypeString(){
        return this.eventType.toString();
    }

    @JsonSetter("event_type")
    public void setEventTypeFromString(String eventType){
        this.eventType = EventType.fromString(eventType);
    }

    public enum TemplateType {
        SLACK,
        MS_TEAMS,
        EMAIL;

        @JsonCreator
        @Nullable
        public static MessageTemplate.TemplateType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(MessageTemplate.TemplateType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}