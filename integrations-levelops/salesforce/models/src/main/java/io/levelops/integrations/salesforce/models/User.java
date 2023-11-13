package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Bean for User <a href="https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_user.htm</a>
 */
@Data
@JsonPropertyOrder({"accountId","alias","city","communityNickname","companyName","contactId","country","createdById",
        "createdDate","department","division","email","employeeNumber","extension","fax","firstName","id",
        "lastModifiedById","lastModifiedDate","lastName","mobilePhone","name","phone","postalCode","profileId","state",
        "systemModstamp","title","userType","username"})
public class User implements SalesforceEntity {

    public static final String SOQL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final List<String> SOQL_FIELDS = Arrays.asList("AccountId", "Alias", "City", "CommunityNickname", "CompanyName", "ContactId", "Country",
            "CreatedById", "CreatedDate", "Department", "Division", "Email", "EmployeeNumber", "Extension", "Fax",
            "FirstName", "Id", "LastModifiedById", "LastModifiedDate", "LastName", "MobilePhone", "Name", "Phone",
            "PostalCode", "ProfileId", "State", "SystemModstamp", "Title", "UserType", "Username");

    @JsonProperty("AccountId")
    String accountId;

    @JsonProperty("Alias")
    String alias;

    @JsonProperty("City")
    String city;

    @JsonProperty("CommunityNickname")
    String communityNickname;

    @JsonProperty("CompanyName")
    String companyName;

    @JsonProperty("ContactId")
    String contactId;

    @JsonProperty("Country")
    String country;

    @JsonProperty("CreatedById")
    String createdById;

    @JsonProperty("CreatedDate")
    Date createdDate;

    @JsonProperty("Department")
    String department;

    @JsonProperty("Division")
    String division;

    @JsonProperty("Email")
    String email;

    @JsonProperty("EmployeeNumber")
    String employeeNumber;

    @JsonProperty("Extension")
    String extension;

    @JsonProperty("Fax")
    String fax;

    @JsonProperty("FirstName")
    String firstName;

    @JsonProperty("Id")
    String id;

    @JsonProperty("LastModifiedById")
    String lastModifiedById;

    @JsonProperty("LastModifiedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date lastModifiedDate;

    @JsonProperty("LastName")
    String lastName;

    @JsonProperty("MobilePhone")
    String mobilePhone;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Phone")
    String phone;

    @JsonProperty("PostalCode")
    String postalCode;

    @JsonProperty("ProfileId")
    String profileId;

    @JsonProperty("State")
    String state;

    @JsonProperty("SystemModstamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = SOQL_DATE_FORMAT)
    Date systemModstamp;

    @JsonProperty("Title")
    String title;

    @JsonProperty("UserType")
    String userType;

    @JsonProperty("Username")
    String username;
}

