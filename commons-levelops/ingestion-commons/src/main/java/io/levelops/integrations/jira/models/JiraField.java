package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraField.JiraFieldBuilder.class)
public class JiraField {

    @JsonProperty("id")
    String id;

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("custom")
    Boolean custom;

    @JsonProperty("orderable")
    Boolean orderable;

    @JsonProperty("navigable")
    Boolean navigable;

    @JsonProperty("searchable")
    Boolean searchable;

    @JsonProperty("clauseNames")
    List<String> clauseNames; // used in JQL

    @JsonProperty("schema")
    Schema schema;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Schema.SchemaBuilder.class)
    public static class Schema {

        @JsonProperty("type")
        String type; // data type (string, number, datetime, ...)

        @JsonProperty("items")
        String items; // for array types (the type of the items)

        @JsonProperty("system")
        String system; // if not custom, name of the system/feature

        //region custom
        @JsonProperty("custom")
        String custom;

        @JsonProperty("customId")
        Integer customId;
        //endregion
    }

}
