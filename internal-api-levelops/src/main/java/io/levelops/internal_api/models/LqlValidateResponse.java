package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.lql.models.LqlAst;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LqlValidateResponse.LqlValidateResponseBuilder.class)
public class LqlValidateResponse {

    @JsonProperty("lql")
    String lql;

    @JsonProperty("ast")
    LqlAst ast;


}
