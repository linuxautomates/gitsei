package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"commentBody","createdBy", "createdById","createdDate","id","isDeleted","isPublished","lastModifiedBy",
        "lastModifiedById", "lastModifiedDate","parentId","systemModstamp"})
public class SalesforceCaseComment{

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @JsonProperty("CommentBody")
    String commentBody;

    @JsonProperty("CreatedById")
    String createdById;

    @JsonProperty("CreatedBy.Name")
    String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    @JsonProperty("CreatedDate")
    Date createdDate;

    @JsonProperty("Id")
    String id;

    @JsonProperty("IsDeleted")
    Boolean isDeleted;

    @JsonProperty("IsPublished")
    Boolean isPublished;

    @JsonProperty("LastModifiedById")
    String lastModifiedById;

    @JsonProperty("LastModifiedBy.Name")
    String lastModifiedBy;

    @JsonProperty("LastModifiedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastModifiedDate;

    @JsonProperty("ParentId")
    String parentId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    @JsonProperty("SystemModstamp")
    Date systemModstamp;
}
