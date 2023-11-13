package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Bean for Contact <a href="https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_contact.htm</a>
 */
@Data
@JsonPropertyOrder({"accountId", "assistantName", "assistantPhone", "birthdate", "createdById", "createdDate", "department",
        "description", "email", "emailBouncedDate", "emailBouncedReason", "fax", "firstName", "homePhone", "id",
        "isDeleted", "isEmailBounced", "jigsaw", "jigsawContactId", "lastActivityDate", "lastCURequestDate",
        "lastCUUpdateDate", "lastModifiedById", "lastModifiedDate", "lastName", "lastReferencedDate", "lastViewedDate",
        "leadSource", "mailingCity", "mailingCountry", "mailingGeocodeAccuracy", "mailingLatitude", "mailingLongitude",
        "mailingPostalCode", "mailingState", "mailingStreet", "masterRecordId", "mobilePhone", "name", "otherCity",
        "otherCountry", "otherGeocodeAccuracy", "otherLatitude", "otherLongitude", "otherPhone", "otherPostalCode",
        "otherState", "otherStreet", "ownerId", "phone", "photoUrl", "reportsToId", "salutation", "systemModstamp", "title"})
public class Contact implements SalesforceEntity {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd";

    public static final List<String> SOQL_FIELDS = Arrays.asList("AccountId", "AssistantName", "AssistantPhone",
            "Birthdate", "CreatedById", "CreatedDate", "Department", "Description", "Email", "EmailBouncedDate",
            "EmailBouncedReason", "Fax", "FirstName", "HomePhone", "Id", "IsDeleted", "IsEmailBounced",
            "Jigsaw", "JigsawContactId", "LastActivityDate", "LastCURequestDate", "LastCUUpdateDate", "LastModifiedById",
            "LastModifiedDate", "LastName", "LastReferencedDate", "LastViewedDate", "LeadSource", "MailingCity", "MailingCountry",
            "MailingGeocodeAccuracy", "MailingLatitude", "MailingLongitude", "MailingPostalCode", "MailingState",
            "MailingStreet", "MasterRecordId", "MobilePhone", "Name", "OtherCity", "OtherCountry", "OtherGeocodeAccuracy",
            "OtherLatitude", "OtherLongitude", "OtherPhone", "OtherPostalCode", "OtherState", "OtherStreet", "OwnerId",
            "Phone", "PhotoUrl", "ReportsToId", "Salutation", "SystemModstamp", "Title");

    @JsonProperty("AccountId")
    String accountId;

    @JsonProperty("AssistantName")
    String assistantName;

    @JsonProperty("AssistantPhone")
    String assistantPhone;

    @JsonProperty("Birthdate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SIMPLE_DATE_FORMAT)
    Date birthdate;

    @JsonProperty("CreatedById")
    String createdById;

    @JsonProperty("CreatedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date createdDate;

    @JsonProperty("Department")
    String department;

    @JsonProperty("Description")
    String description;

    @JsonProperty("Email")
    String email;

    @JsonProperty("EmailBouncedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date emailBouncedDate;

    @JsonProperty("EmailBouncedReason")
    String emailBouncedReason;

    @JsonProperty("Fax")
    String fax;

    @JsonProperty("FirstName")
    String firstName;

    @JsonProperty("HomePhone")
    String homePhone;

    @JsonProperty("Id")
    String id;

    @JsonProperty("IsDeleted")
    Boolean isDeleted;

    @JsonProperty("IsEmailBounced")
    Boolean isEmailBounced;

    @JsonProperty("Jigsaw")
    String jigsaw;

    @JsonProperty("JigsawContactId")
    String jigsawContactId;

    @JsonProperty("LastActivityDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SIMPLE_DATE_FORMAT)
    Date lastActivityDate;

    @JsonProperty("LastCURequestDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastCURequestDate;

    @JsonProperty("LastCUUpdateDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastCUUpdateDate;

    @JsonProperty("LastModifiedById")
    String lastModifiedById;

    @JsonProperty("LastModifiedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastModifiedDate;

    @JsonProperty("LastName")
    String lastName;

    @JsonProperty("LastReferencedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastReferencedDate;

    @JsonProperty("LastViewedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastViewedDate;

    @JsonProperty("LeadSource")
    String leadSource;

    @JsonProperty("MailingCity")
    String mailingCity;

    @JsonProperty("MailingCountry")
    String mailingCountry;

    @JsonProperty("MailingGeocodeAccuracy")
    String mailingGeocodeAccuracy;

    @JsonProperty("MailingLatitude")
    String mailingLatitude;

    @JsonProperty("MailingLongitude")
    String mailingLongitude;

    @JsonProperty("MailingPostalCode")
    String mailingPostalCode;

    @JsonProperty("MailingState")
    String mailingState;

    @JsonProperty("MailingStreet")
    String mailingStreet;

    @JsonProperty("MasterRecordId")
    String masterRecordId;

    @JsonProperty("MobilePhone")
    String mobilePhone;

    @JsonProperty("Name")
    String name;

    @JsonProperty("OtherCity")
    String otherCity;

    @JsonProperty("OtherCountry")
    String otherCountry;

    @JsonProperty("OtherGeocodeAccuracy")
    String otherGeocodeAccuracy;

    @JsonProperty("OtherLatitude")
    String otherLatitude;

    @JsonProperty("OtherLongitude")
    String otherLongitude;

    @JsonProperty("OtherPhone")
    String otherPhone;

    @JsonProperty("OtherPostalCode")
    String otherPostalCode;

    @JsonProperty("OtherState")
    String otherState;

    @JsonProperty("OtherStreet")
    String otherStreet;

    @JsonProperty("OwnerId")
    String ownerId;

    @JsonProperty("Phone")
    String phone;

    @JsonProperty("PhotoUrl")
    String photoUrl;

    @JsonProperty("ReportsToId")
    String reportsToId;

    @JsonProperty("Salutation")
    String salutation;

    @JsonProperty("SystemModstamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date systemModstamp;

    @JsonProperty("Title")
    String title;
}
