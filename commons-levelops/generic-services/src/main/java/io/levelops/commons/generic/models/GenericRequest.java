package io.levelops.commons.generic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * In Server Api and Internal Api we will have a Generic Request Api.
 * This api takes {@link GenericRequest} as input and return {@link GenericResponse}.
 * For well defined data e.g. Plugin Results etc.. users will use the well defined end points.
 * For short lived, time sensetive adhoc events this end point is preferred.
 * e.g. Jenkins Plugin detects new build and call this end point to see if build should be blocked.
 * {@link GenericRequest} has only one specific field request_type and one generic field payload.
 * Each type of Generic Request will define its own format for payload and will know how to deserialize it.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GenericRequest.GenericRequestBuilder.class)
public class GenericRequest {
    @JsonProperty("request_type")
    private final String requestType;
    @JsonProperty("payload")
    private final String payload;
}