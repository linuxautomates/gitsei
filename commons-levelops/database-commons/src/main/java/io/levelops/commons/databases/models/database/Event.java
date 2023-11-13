package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonDeserialize(builder = Event.EventBuilderImpl.class)
public class Event {
    private String company;
    private EventType type;
    private Map<String, Object> data;

    @JsonGetter("type")
    public String getTypeString(){
        return this.type.toString();
    }
    @JsonPOJOBuilder(withPrefix = "")
    public static class EventBuilderImpl extends EventBuilder{
        public EventBuilderImpl type(String eventType){
            this.type(EventType.fromString(eventType));
            return this;
        }
    }
}