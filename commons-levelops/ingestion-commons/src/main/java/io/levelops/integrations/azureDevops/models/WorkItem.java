package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItem.WorkItemBuilder.class)
public class WorkItem {

    @JsonProperty("id")
    String id;

    @JsonProperty("rev")
    Integer rev;

    @JsonProperty("url")
    String url;

    @JsonProperty("fields")
    Fields fields;

    @JsonProperty("_links")
    Link _links;

    @JsonProperty("relations")
    List<WorkItemRelation> relations;

    @JsonProperty("commentVersionRef")
    WorkItemCommentVersionRef commentVersionRef;

    @JsonProperty("comments")
    List<Comment> comments;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemRelation.WorkItemRelationBuilder.class)
    public static class WorkItemRelation {

        @JsonProperty("rel")
        String rel;

        @JsonProperty("url")
        String url;

        @JsonProperty("attributes")
        Attributes attributes;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = WorkItemRelation.Attributes.AttributesBuilder.class)
        public static class Attributes {

            @JsonProperty("name")
            String name;

            @JsonProperty("id")
            Integer id;

            @JsonProperty("resourceSize")
            Integer resourceSize;

            @JsonProperty("authorizedDate")
            String authorizedDate;

            @JsonProperty("resourceCreatedDate")
            String resourceCreatedDate;

            @JsonProperty("resourceModifiedDate")
            String resourceModifiedDate;

            @JsonProperty("revisedDate")
            String revisedDate;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemCommentVersionRef.WorkItemCommentVersionRefBuilder.class)
    private static class WorkItemCommentVersionRef {

        @JsonProperty("commentId")
        Integer commentId;

        @JsonProperty("createdInRevision")
        Integer createdInRevision;

        @JsonProperty("text")
        String text;

        @JsonProperty("url")
        String url;

        @JsonProperty("isDeleted")
        Boolean isDeleted;

        @JsonProperty("version")
        Integer version;
    }
}
