package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OktaUser.OktaUserBuilder.class)
public class OktaUser {

    @JsonProperty
    String id;

    @JsonProperty
    String status;

    @JsonProperty
    Date created;

    @JsonProperty
    Date activated;

    @JsonProperty("statusChanged")
    Date statusChanged;

    @JsonProperty("lastLogin")
    Date lastLogin;

    @JsonProperty("lastUpdated")
    Date lastUpdated;

    @JsonProperty("passwordChanged")
    Date passwordChanged;

    @JsonProperty
    Map<String, String> type;

    @JsonProperty("transitioningToStatus")
    String transitioningToStatus;

    @JsonProperty
    Profile profile;

    @JsonProperty("_links")
    Map<String, OktaHref> links;

    @JsonProperty
    List<String> groups; //enriched

    @JsonProperty("linkedMembers")
    List<AssociatedMembers> associatedMembers; //enriched

    @JsonProperty
    OktaUserType enrichedUserType;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Profile.ProfileBuilder.class)
    public static class Profile {

        @JsonProperty
        String login;

        @JsonProperty
        String email;

        @JsonProperty("secondEmail")
        String secondEmail;

        @JsonProperty("firstName")
        String firstName;

        @JsonProperty("lastName")
        String lastName;

        @JsonProperty("middleName")
        String middleName;

        @JsonProperty("honorificSuffix")
        String honorificSuffix;

        @JsonProperty("honorificPrefix")
        String honorificPrefix;

        @JsonProperty
        String title;

        @JsonProperty("displayName")
        String displayName;

        @JsonProperty("nickName")
        String nickName;

        @JsonProperty("profileUrl")
        String profileUrl;

        @JsonProperty("primaryPhone")
        String primaryPhone;

        @JsonProperty("mobilePhone")
        String mobilePhone;

        @JsonProperty("streetAddress")
        String streetAddress;

        @JsonProperty("city")
        String city;

        @JsonProperty("state")
        String state;

        @JsonProperty("zipCode")
        String zipCode;

        @JsonProperty("countryCode")
        String countryCode;

        @JsonProperty("postalAddress")
        String postalAddress;

        @JsonProperty("preferredLanguage")
        String preferredLanguage;

        @JsonProperty("locale")
        String locale;

        @JsonProperty("timezone")
        String timezone;

        @JsonProperty("userType")
        String userType;

        @JsonProperty("employeeNumber")
        String employeeNumber;

        @JsonProperty("costCenter")
        String costCenter;

        @JsonProperty
        String organization;

        @JsonProperty
        String division;

        @JsonProperty("department")
        String department;

        @JsonProperty("managerId")
        String managerId;

        @JsonProperty("manager")
        String manager;
    }
}
