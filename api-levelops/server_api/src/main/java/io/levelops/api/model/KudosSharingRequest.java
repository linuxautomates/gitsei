package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = KudosSharingRequest.KudosSharingRequestBuilder.class)
public class KudosSharingRequest {
    @JsonProperty("channels")
    Set<String> channels;
    @JsonProperty("emails")
    Set<String> emails;
}
