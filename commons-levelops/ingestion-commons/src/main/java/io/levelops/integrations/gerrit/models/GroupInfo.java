package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bean describing a Group from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-groups.html#group-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GroupInfo.GroupInfoBuilder.class)
public class GroupInfo {

    @JsonProperty
    String id;

    @JsonProperty
    String name;

    @JsonProperty
    String url;

    @JsonProperty
    GroupOptionsInfo options;

    @JsonProperty
    String description;

    @JsonProperty("group_id")
    Integer groupId;

    @JsonProperty
    String owner;

    @JsonProperty("owner_id")
    String ownerId;

    @JsonProperty("created_on")
    LocalDateTime createdOn;

    @JsonProperty("_more_groups")
    Boolean moreGroups;

    @JsonProperty
    List<AccountInfo> members;

    @JsonProperty
    List<GroupInfo> includes;

    /**
     * Bean describing a GroupOptions from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-groups.html#group-options-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GroupOptionsInfo.GroupOptionsInfoBuilder.class)
    public static class GroupOptionsInfo {

        @JsonProperty(value = "visible_to_all", defaultValue = "false")
        Boolean visibleToAll;
    }

}
