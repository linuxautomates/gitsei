package io.levelops.commons.databases.models.database.okta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaUser;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbOktaGroup.DbOktaGroupBuilder.class)
public class DbOktaGroup {

    @JsonProperty
    String id;

    @JsonProperty("group_id")
    String groupId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("object_class")
    List<String> objectClass;

    @JsonProperty
    String name;

    @JsonProperty
    String description;

    @JsonProperty
    String type;

    @JsonProperty
    List<String> members;

    @JsonProperty
    Date lastUpdatedAt;

    public static DbOktaGroup fromOktaGroup(OktaGroup group, String integrationId, Date currentTime) {
        return DbOktaGroup.builder()
                .integrationId(integrationId)
                .description(group.getProfile().getDescription())
                .groupId(group.getId())
                .members(group.getEnrichedUsers() != null ?
                        group.getEnrichedUsers().stream().map(OktaUser::getId).collect(Collectors.toList()) : Collections.emptyList())
                .name(group.getProfile().getName())
                .objectClass(group.getObjectClass())
                .type(group.getType())
                .lastUpdatedAt(currentTime)
                .build();
    }
}
