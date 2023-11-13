package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Bean for Case history <a href="https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_casehistory.htm</a>
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"contact", "status", "caseId", "createdById", "createdDate", "field", "id", "isDeleted", "newValue", "oldValue"})
public class SalesforceCaseHistory {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @JsonProperty("Id")
    String id;

    @JsonProperty("IsDeleted")
    Boolean isDeleted;

    @JsonProperty("CaseId")
    String caseId;

    @JsonProperty("Case.ContactEmail")
    String contact;

    @JsonProperty("Case.Status")
    String status;

    @JsonProperty("CreatedById")
    String createdById;

    @JsonProperty("CreatedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date createdDate;

    @JsonProperty("Field")
    String field;

    @JsonProperty("OldValue")
    String oldValue;

    @JsonProperty("NewValue")
    String newValue;
}
