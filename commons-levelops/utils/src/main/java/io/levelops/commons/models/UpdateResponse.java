package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonDeserialize(builder = UpdateResponse.UpdateResponseBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("error")
    private String error;

    public UpdateResponse build(String id, Boolean success, String errorMessage) {
        return UpdateResponse.builder()
                .id(id)
                .success(success)
                .error(errorMessage).build();
    }
}