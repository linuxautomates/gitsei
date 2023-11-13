package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckMetaDataFields.BlackDuckMetaDataFieldsBuilder.class)
@JsonIgnoreProperties(value={ "rel", "href" }, allowSetters= true)
public class BlackDuckMetaDataFields {

    @JsonProperty("rel")
    String relName;

    @JsonProperty("href")
    String relHref;
}
