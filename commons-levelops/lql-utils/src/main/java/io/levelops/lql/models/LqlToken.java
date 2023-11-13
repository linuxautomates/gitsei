package io.levelops.lql.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonDeserialize(builder = LqlToken.LqlTokenBuilder.class)
public class LqlToken {
    @JsonProperty("type")
    LqlTokenType type;

    @JsonProperty("value")
    String value;

    @JsonProperty("raw_value")
    String rawValue;

    public LqlToken(LqlTokenType type, String rawValue) {
        this.type = type;
        this.rawValue = rawValue;
        this.value = rawValue;
    }

    @Override
    public String toString() {
        return type.toString() + "<" + value + ">";
    }
}