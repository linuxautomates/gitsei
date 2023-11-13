package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckMetadata.BlackDuckMetadataBuilder.class)
@JsonIgnoreProperties(value={ "allow", "href", "links" }, allowSetters= true)
public class BlackDuckMetadata {

    @JsonProperty("allow")
    List<String> allowedOperations;

    @JsonProperty("href")
    String projectHref;

    @JsonProperty("links")
    List<BlackDuckMetaDataFields> blackDuckMetaDataFields;
}
