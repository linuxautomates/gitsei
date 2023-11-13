package io.levelops.commons.databases.models.database.okta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.okta.models.OktaUser;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbOktaUser.DbOktaUserBuilder.class)
public class DbOktaUser {

    public static final String DEFAULT = "default";
    @JsonProperty
    String id;

    @JsonProperty
    String userId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty
    String status;

    @JsonProperty("user_type_name")
    String userTypeName;

    @JsonProperty("user_type_display_name")
    String userTypeDisplayName;

    @JsonProperty("user_type_description")
    String userTypeDescription;

    @JsonProperty("transitioning_to_status")
    String transitioningToStatus;

    @JsonProperty
    String login;

    @JsonProperty
    String email;

    @JsonProperty("first_name")
    String firstName;

    @JsonProperty("middle_name")
    String middleName;

    @JsonProperty("last_name")
    String lastName;

    @JsonProperty
    String title;

    @JsonProperty("display_name")
    String displayName;

    @JsonProperty("nick_name")
    String nickName;

    @JsonProperty("time_zone")
    String timeZone;

    @JsonProperty("employee_number")
    String employeeNumber;

    @JsonProperty("cost_center")
    String costCenter;

    @JsonProperty
    String organisation;

    @JsonProperty
    String division;

    @JsonProperty
    String department;

    @JsonProperty("manager_id")
    String managerId;

    @JsonProperty("manager")
    String manager;

    @JsonProperty
    List<String> groups;

    @JsonProperty
    Date lastUpdatedAt;

    public static DbOktaUser fromOktaUSer(OktaUser oktaUser, String integrationId, Date currentTime) {
        return DbOktaUser.builder()
                .userId(oktaUser.getId())
                .integrationId(integrationId)
                .status(oktaUser.getStatus())
                .userTypeName(oktaUser.getEnrichedUserType() != null ?
                        oktaUser.getEnrichedUserType().getName() : DEFAULT)
                .userTypeDisplayName(oktaUser.getEnrichedUserType() != null ?
                        oktaUser.getEnrichedUserType().getDisplayName() : DEFAULT)
                .userTypeDescription(oktaUser.getEnrichedUserType() != null ?
                        oktaUser.getEnrichedUserType().getDescription() : DEFAULT)
                .transitioningToStatus(oktaUser.getTransitioningToStatus())
                .login(oktaUser.getProfile().getLogin())
                .email(oktaUser.getProfile().getEmail())
                .firstName(oktaUser.getProfile().getFirstName())
                .middleName(oktaUser.getProfile().getMiddleName())
                .lastName(oktaUser.getProfile().getLastName())
                .title(oktaUser.getProfile().getTitle())
                .displayName(oktaUser.getProfile().getDisplayName())
                .nickName(oktaUser.getProfile().getNickName())
                .timeZone(oktaUser.getProfile().getTimezone())
                .employeeNumber(oktaUser.getProfile().getEmployeeNumber())
                .costCenter(oktaUser.getProfile().getCostCenter())
                .organisation(oktaUser.getProfile().getOrganization())
                .division(oktaUser.getProfile().getDivision())
                .department(oktaUser.getProfile().getDepartment())
                .manager(oktaUser.getProfile().getManager())
                .managerId(oktaUser.getProfile().getManagerId())
                .groups(oktaUser.getGroups())
                .lastUpdatedAt(currentTime)
                .build();
    }
}
