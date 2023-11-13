package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbScmPRLabelLite.DbScmPRLabelLiteBuilder.class)
public class DbScmPRLabelLite {
    @JsonProperty("name")
    private String name;
}
