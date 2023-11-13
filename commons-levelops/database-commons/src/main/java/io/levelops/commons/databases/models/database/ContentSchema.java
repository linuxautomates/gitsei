package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.ValueType;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ContentSchema.ContentSchemaBuilder.class)
public class ContentSchema {

    @JsonProperty("key")
    String key;
    @JsonProperty("value_type")
    ValueType valueType;
    @JsonProperty("content_type")
    ContentType contentType;
    @JsonProperty("fields")
    Map<String, ContentSchema> fields;

}
