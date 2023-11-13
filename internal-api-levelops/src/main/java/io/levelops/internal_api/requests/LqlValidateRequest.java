package io.levelops.internal_api.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LqlValidateRequest.LqlValidateRequestBuilder.class)
public class LqlValidateRequest {

    @JsonProperty("lqls")
    List<String> lqls;

}
