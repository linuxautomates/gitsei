package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Bean describing a Change from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#change-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewerInfo.ReviewerInfoBuilder.class)
public class ReviewerInfo {

    @JsonProperty
    Map<String, String> approvals;

    @JsonProperty("_account_id")
    String accountId;

    @JsonProperty
    String name;

    @JsonProperty("display_name")
    String displayName;

    @JsonProperty("email")
    String email;

    @JsonProperty("seconday_emails")
    List<String> secondaryEmails;

    @JsonProperty
    String username;

    @JsonProperty
    List<AccountInfo.AvatarInfo> avatars;

    @JsonProperty
    Boolean moreAccounts;

    @JsonProperty
    String status;

    @JsonProperty(defaultValue = "false")
    Boolean inactive;
}
