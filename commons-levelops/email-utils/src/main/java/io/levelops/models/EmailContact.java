package io.levelops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EmailContact.EmailContactBuilder.class)
public class EmailContact {

    @JsonProperty("name")
    String name;
    @JsonProperty("email")
    String email;

}
