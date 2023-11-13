package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemQueryResult.WorkItemQueryResultBuilder.class)
public class WorkItemQueryResult {

    @JsonProperty("asof")
    String asof;

    @JsonProperty("columns")
    List<WorkItemFieldReference> columns;

    @JsonProperty("queryResultType")
    String queryResultType;

    @JsonProperty("queryType")
    String queryType;

    @JsonProperty("sortColumns")
    List<WorkItemQuerySortColumn> sortColumns;

    @JsonProperty("workItems")
    List<WorkItemReference> workItems;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemFieldReference.WorkItemFieldReferenceBuilder.class)
    private static class WorkItemFieldReference {

        @JsonProperty("rel")
        String rel;

        @JsonProperty("url")
        String url;

        @JsonProperty("attributes")
        Attributes attributes;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemQuerySortColumn.WorkItemQuerySortColumnBuilder.class)
    private static class WorkItemQuerySortColumn {

        @JsonProperty("field")
        WorkItemFieldReference field;

        @JsonProperty("descending")
        Boolean descending;


        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = WorkItemFieldReference.WorkItemFieldReferenceBuilder.class)
        private static class WorkItemFieldReference {

            @JsonProperty("name")
            String name;

            @JsonProperty("referenceName")
            String referenceName;

            @JsonProperty("url")
            String url;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemReference.WorkItemReferenceBuilder.class)
    public static class WorkItemReference {

        @JsonProperty("id")
        Integer id;

        @JsonProperty("url")
        String url;
    }
}
