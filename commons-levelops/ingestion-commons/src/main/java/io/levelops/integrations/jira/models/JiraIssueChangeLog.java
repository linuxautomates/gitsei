package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraIssueChangeLog.JiraIssueChangeLogBuilder.class)
public class JiraIssueChangeLog {

    @JsonProperty("self")
    String self;
    @JsonProperty("maxResults")
    Long maxResults;
    @JsonProperty("startAt")
    Long startAt;
    @JsonProperty("total")
    Long total;
    @JsonProperty("isLast")
    Boolean isLast;

    @JsonProperty("values") // using /changelog enpoint
    List<ChangeLogEvent> values;

    @JsonProperty("histories") // using search expand
    List<ChangeLogEvent> histories;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ChangeLogEvent.ChangeLogEventBuilder.class)
    public static class ChangeLogEvent {
        @JsonProperty("id")
        String id;
        @JsonProperty("author")
        JiraUser author;
        @JsonProperty("created")
        Date created;

        @JsonProperty("items")
        List<ChangeLogDetails> items;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ChangeLogDetails.ChangeLogDetailsBuilder.class)
    public static class ChangeLogDetails {
        @JsonProperty("field")
        String field;
        @JsonProperty("fieldtype")
        String fieldtype;
        @JsonProperty("fieldId")
        String fieldId;
        @JsonProperty("from")
        String from;
        @JsonProperty("fromString")
        String fromString;
        @JsonProperty("to")
        String to;
        @JsonProperty("toString")
        String toString;
        @JsonProperty("tmpFromAccountId")
        String tmpFromAccountId;
        @JsonProperty("tmpToAccountId")
        String tmpToAccountId;
    }
}
