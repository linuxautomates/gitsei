package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean describing a Group from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-groups.html#group-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AccountInfo.AccountInfoBuilder.class)
public class AccountInfo {

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
    List<AvatarInfo> avatars;

    @JsonProperty
    Boolean moreAccounts;

    @JsonProperty
    String status;

    @JsonProperty(defaultValue = "false")
    Boolean inactive;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AvatarInfo.AvatarInfoBuilder.class)
    public static class AvatarInfo {
        @JsonProperty
        String url;

        @JsonProperty
        String height;

        @JsonProperty
        String width;
    }
}
