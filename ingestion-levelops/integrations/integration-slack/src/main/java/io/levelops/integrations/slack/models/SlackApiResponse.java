package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class SlackApiResponse<T> {

    @JsonProperty("ok")
    private Boolean ok;

    @JsonUnwrapped
    private T payload;

    private Map<String, Object> dynamicProperties;

    @JsonIgnore
    public boolean isOk() {
        return Boolean.TRUE.equals(ok);
    }

    @JsonAnySetter
    public void setDynamicProperty(String propertyKey, Object value) {
        if (this.dynamicProperties == null) {
            this.dynamicProperties = new HashMap<>();
        }
        this.dynamicProperties.put(propertyKey, value);
    }

}