package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Bean for Solution <a href="https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_solution.htm</a>
 */
@Data
@JsonPropertyOrder({"createdBy", "createdById", "createdDate", "id", "isDeleted", "isHtml", "isPublished", "isPublishedInPublicKb",
        "isReviewed", "lastModifiedBy", "lastModifiedById", "lastModifiedDate", "lastReferencedDate", "lastViewedDate", "ownerId",
        "solutionName", "solutionNote", "solutionNumber", "status", "systemModstamp", "timesUsed"})
public class Solution implements SalesforceEntity {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final List<String> SOQL_FIELDS = Arrays.asList("CreatedById", "CreatedDate", "Id", "IsDeleted", "IsHtml", "IsPublished",
            "IsPublishedInPublicKb", "IsReviewed", "LastModifiedById", "LastModifiedDate", "LastReferencedDate",
            "LastViewedDate", "OwnerId", "SolutionName", "SolutionNote", "SolutionNumber", "Status", "SystemModstamp", "TimesUsed");

    @JsonProperty("Id")
    String id;

    @JsonProperty("IsDeleted")
    Boolean isDeleted;

    @JsonProperty("SolutionNumber")
    String solutionNumber;

    @JsonProperty("SolutionName")
    String solutionName;

    @JsonProperty("IsPublished")
    Boolean isPublished;

    @JsonProperty("IsPublishedInPublicKb")
    Boolean isPublishedInPublicKb;

    @JsonProperty("Status")
    String status;

    @JsonProperty("IsReviewed")
    Boolean isReviewed;

    @JsonProperty("SolutionNote")
    String solutionNote;

    @JsonProperty("OwnerId")
    String ownerId;

    @JsonProperty("CreatedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date createdDate;

    @JsonProperty("CreatedById")
    String createdById;

    @JsonProperty("LastModifiedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastModifiedDate;

    @JsonProperty("LastModifiedById")
    String lastModifiedById;

    @JsonProperty("SystemModstamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date systemModstamp;

    @JsonProperty("TimesUsed")
    Integer timesUsed;

    @JsonProperty("LastViewedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastViewedDate;

    @JsonProperty("LastReferencedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastReferencedDate;

    @JsonProperty("IsHtml")
    Boolean isHtml;

    @JsonUnwrapped(prefix = "CreatedBy.")
    User createdBy;

    @JsonUnwrapped(prefix = "LastModifiedBy.")
    User lastModifiedBy;
}
