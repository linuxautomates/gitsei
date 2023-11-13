package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GitTechnology {
    @JsonProperty("id")
    private String id;

    @JsonProperty("repo_id")
    private String repoId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("created_at")
    private Long createdAt;
}