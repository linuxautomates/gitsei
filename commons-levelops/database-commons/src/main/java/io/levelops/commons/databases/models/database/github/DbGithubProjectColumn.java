package io.levelops.commons.databases.models.database.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.github.models.GithubProjectColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbGithubProjectColumn {

    @JsonProperty("id")
    private String id;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("column_id")
    private String columnId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("column_created_at")
    private Long columnCreatedAt;

    @JsonProperty("column_updated_at")
    private Long columnUpdatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    public static DbGithubProjectColumn fromProjectColumn(GithubProjectColumn source, String projectId) {
        return  DbGithubProjectColumn.builder()
                .projectId(projectId)
                .columnId(source.getId())
                .name(source.getName())
                .columnCreatedAt(source.getCreatedAt().toInstant().getEpochSecond())
                .columnUpdatedAt(source.getUpdatedAt().toInstant().getEpochSecond())
                .build();
    }

}