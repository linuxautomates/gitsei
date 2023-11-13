package io.levelops.notification.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackApiViewResponse.SlackApiViewResponseBuilder.class)
public class SlackApiViewResponse {

    @JsonProperty("view")
    SlackApiView view;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(SlackApiResponse.class, SlackApiViewResponse.class);
    }
}
