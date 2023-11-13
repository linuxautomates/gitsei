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
 * Bean for Case entity <a href="https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_case.htm</a>
 */
@Data
@JsonPropertyOrder({"accountName", "accountId", "caseNumber", "closedDate", "comments", "contact", "contactEmail",
        "contactFax", "contactId", "contactMobile", "contactPhone", "createdBy", "createdById",
        "createdDate", "description", "id", "isClosed", "isDeleted", "isEscalated", "lastModifiedBy", "lastModifiedById",
        "lastModifiedDate", "lastReferencedDate", "lastViewedDate", "masterRecordId", "origin", "ownerId",
        "priority", "reason", "status", "subject", "suppliedEmail", "systemModstamp", "type"})
public class Case implements SalesforceEntity {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final List<String> SOQL_FIELDS = Arrays.asList("Account.Name", "AccountId", "CaseNumber", "ClosedDate",
            "Comments", "ContactEmail", "ContactFax", "ContactId", "ContactMobile", "ContactPhone", "CreatedById",
            "CreatedDate", "Description", "Id", "IsClosed", "IsDeleted", "IsEscalated", "LastModifiedById", "LastModifiedDate",
            "LastReferencedDate", "LastViewedDate", "MasterRecordId", "Origin", "OwnerId", "Priority", "Reason",
            "Status", "Subject", "SuppliedEmail", "SystemModstamp", "Type");

    @JsonProperty("Id")
    String id;

    @JsonProperty("IsDeleted")
    Boolean isDeleted;

    @JsonProperty("MasterRecordId")
    String masterRecordId;

    @JsonProperty("CaseNumber")
    String caseNumber;

    @JsonProperty("ContactId")
    String contactId;

    @JsonProperty("Account.Name")
    String accountName;

    @JsonProperty("AccountId")
    String accountId;

//    @JsonProperty("ParentId")
//    String parentId;

    @JsonProperty("SuppliedEmail")
    String suppliedEmail;

    @JsonProperty("Type")
    String type;

    @JsonProperty("Status")
    String status;

    @JsonProperty("Reason")
    String reason;

    @JsonProperty("Origin")
    String origin;

    @JsonProperty("Subject")
    String subject;

    @JsonProperty("Priority")
    String priority;

    @JsonProperty("Description")
    String description;

    @JsonProperty("IsClosed")
    Boolean isClosed;

    @JsonProperty("ClosedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date closedDate;

    @JsonProperty("IsEscalated")
    Boolean isEscalated;

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

    @JsonProperty("ContactPhone")
    String contactPhone;

    @JsonProperty("ContactMobile")
    String contactMobile;

    @JsonProperty("ContactEmail")
    String contactEmail;

    @JsonProperty("ContactFax")
    String contactFax;

    @JsonProperty("Comments")
    String comments;

    @JsonProperty("LastViewedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastViewedDate;

    @JsonProperty("LastReferencedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastReferencedDate;

//    @JsonUnwrapped(prefix = "Contact.")
//    Contact contact;

    @JsonUnwrapped(prefix = "Owner.")
    Contact owner;

    @JsonUnwrapped(prefix = "CreatedBy.")
    User createdBy;

    @JsonUnwrapped(prefix = "LastModifiedBy.")
    User lastModifiedBy;
}