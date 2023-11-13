package io.levelops.commons.databases.models.database.checkmarx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbCxSastProject.DbCxSastProjectBuilder.class)
public class DbCxSastProject {
    public static final String EMPTY = "";
    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("team_id")
    String teamId;

    @JsonProperty("name")
    String name;

    @JsonProperty("is_public")
    Boolean isPublic;

    public static DbCxSastProject fromProject(CxSastProject source,
                                              String integrationId) {
        return DbCxSastProject.builder()
                .integrationId(integrationId)
                .projectId(source.getId())
                .teamId(MoreObjects.firstNonNull(source.getTeamId(), EMPTY))
                .name(MoreObjects.firstNonNull(source.getName(), EMPTY))
                .isPublic(MoreObjects.firstNonNull(source.isPublic(), false))
                .build();
    }
}
