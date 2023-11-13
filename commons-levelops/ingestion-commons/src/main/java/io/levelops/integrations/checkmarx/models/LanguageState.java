package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LanguageState.LanguageStateBuilder.class)
public class LanguageState {

    @JsonProperty("languageId")
    String languageId;

    @JsonProperty("languageName")
    String languageName;

    @JsonProperty("languageHash")
    String languageHash;
}
