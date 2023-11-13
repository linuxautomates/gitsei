package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = UserGroupResponse.UserGroupResponseBuilder.class)
public class UserGroupResponse {

    @JsonProperty("paging")
    Paging paging;

    @JsonProperty("groups")
    List<Group> groups;
}