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
 * Bean for Service contract <a href="https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_servicecontract.htm</a>
 */
@Data
@JsonPropertyOrder({"accountId", "activatedBy", "activatedById", "activatedDate", "billingCity", "billingCountry", "billingGeocodeAccuracy",
        "billingLatitude", "billingLongitude", "billingPostalCode", "billingState", "billingStreet", "companySignedDate",
        "companySignedId", "contractNumber", "contractTerm", "createdBy", "createdById", "createdDate", "customerSignedDate",
        "customerSignedId", "customerSignedTitle", "description", "endDate", "id", "isDeleted", "lastActivityDate",
        "lastApprovedDate", "lastModifiedBy", "lastModifiedById", "lastModifiedDate", "lastReferencedDate", "lastViewedDate",
        "ownerExpirationNotice", "ownerId", "specialTerms", "startDate", "status", "statusCode", "systemModstamp"})
public class Contract implements SalesforceEntity {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final List<String> SOQL_FIELDS = Arrays.asList("AccountId", "ActivatedById", "ActivatedDate", "BillingCity", "BillingCountry",
            "BillingGeocodeAccuracy", "BillingLatitude", "BillingLongitude", "BillingPostalCode", "BillingState",
            "BillingStreet", "CompanySignedDate", "CompanySignedId", "ContractNumber", "ContractTerm",
            "CreatedById", "CreatedDate", "CustomerSignedDate", "CustomerSignedId", "CustomerSignedTitle",
            "Description", "EndDate", "Id", "IsDeleted", "LastActivityDate", "LastApprovedDate", "LastModifiedById",
            "LastModifiedDate", "LastReferencedDate", "LastViewedDate", "OwnerExpirationNotice", "OwnerId",
            "SpecialTerms", "StartDate", "Status", "StatusCode", "SystemModstamp");

    @JsonProperty("AccountId")
    String accountId;

    @JsonProperty("ActivatedById")
    String activatedById;

    @JsonProperty("ActivatedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date activatedDate;

    @JsonProperty("BillingCity")
    String billingCity;

    @JsonProperty("BillingCountry")
    String billingCountry;

    @JsonProperty("BillingGeocodeAccuracy")
    String billingGeocodeAccuracy;

    @JsonProperty("BillingLatitude")
    String billingLatitude;

    @JsonProperty("BillingLongitude")
    String billingLongitude;

    @JsonProperty("BillingPostalCode")
    String billingPostalCode;

    @JsonProperty("BillingState")
    String billingState;

    @JsonProperty("BillingStreet")
    String billingStreet;

    @JsonProperty("CompanySignedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date companySignedDate;

    @JsonProperty("CompanySignedId")
    String companySignedId;

    @JsonProperty("ContractNumber")
    String contractNumber;

    @JsonProperty("ContractTerm")
    String contractTerm;

    @JsonProperty("CreatedById")
    String createdById;

    @JsonProperty("CreatedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date createdDate;

    @JsonProperty("CustomerSignedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date customerSignedDate;

    @JsonProperty("CustomerSignedId")
    String customerSignedId;

    @JsonProperty("CustomerSignedTitle")
    String customerSignedTitle;

    @JsonProperty("Description")
    String description;

    @JsonProperty("EndDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    Date endDate;

    @JsonProperty("Id")
    String id;

    @JsonProperty("IsDeleted")
    Boolean isDeleted;

    @JsonProperty("LastActivityDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastActivityDate;

    @JsonProperty("LastApprovedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastApprovedDate;

    @JsonProperty("LastModifiedById")
    String lastModifiedById;

    @JsonProperty("LastModifiedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastModifiedDate;

    @JsonProperty("LastReferencedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastReferencedDate;

    @JsonProperty("LastViewedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastViewedDate;

    @JsonProperty("OwnerExpirationNotice")
    String ownerExpirationNotice;

    @JsonProperty("OwnerId")
    String ownerId;

    @JsonProperty("SpecialTerms")
    String specialTerms;

    @JsonProperty("StartDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    Date startDate;

    @JsonProperty("Status")
    String status;

    @JsonProperty("StatusCode")
    String statusCode;

    @JsonProperty("SystemModstamp")
    String systemModstamp;

    @JsonUnwrapped(prefix = "ActivatedBy.")
    User activatedBy;

    @JsonUnwrapped(prefix = "CreatedBy.")
    User createdBy;

    @JsonUnwrapped(prefix = "LastModifiedBy.")
    User lastModifiedBy;
}