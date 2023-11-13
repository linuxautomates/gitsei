package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OktaGroup.OktaGroupBuilder.class)
public class OktaGroup {

    @JsonProperty
    String id;

    @JsonProperty
    Date created;

    @JsonProperty("lastUpdated")
    Date lastUpdated;

    @JsonProperty("lastMembershipUpdated")
    Date lastMembershipUpdated;

    @JsonProperty("objectClass")
    List<String> objectClass;

    @JsonProperty
    String type;

    @JsonProperty
    Profile profile;

    @JsonProperty("_links")
    Links links;

    @JsonProperty("enrichedUsers")
    List<OktaUser> enrichedUsers; //enriched

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Profile.ProfileBuilder.class)
    public static class Profile {

        @JsonProperty
        String name;

        @JsonProperty
        String description;

        @JsonProperty("groupType")
        String groupType;

        @JsonProperty("samAccountName")
        String samAccountName;

        @JsonProperty("objectSid")
        String objectSid;

        @JsonProperty("groupScope")
        String groupScope;

        @JsonProperty("dn")
        String dn;

        @JsonProperty("windowsDomainQualifiedName")
        String windowsDomainQualifiedName;

        @JsonProperty("externalId")
        String externalId;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Links.LinksBuilder.class)
    public static class Links {

        @JsonProperty
        List<OktaHref> logo;

        @JsonProperty
        OktaHref users;

        @JsonProperty
        OktaHref apps;
    }
}
