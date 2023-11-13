package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabChange.GitlabChangeBuilder.class)
public class GitlabChange {
    @JsonProperty("old_path")
    String oldPath;
    @JsonProperty("new_path")
    String newPath;
    @JsonProperty("a_mode")
    String aMode;
    @JsonProperty("b_mode")
    String bMode;
    @JsonProperty("new_file")
    boolean newFile;
    @JsonProperty("renamed_file")
    boolean renamedFile;
    @JsonProperty("deleted_file")
    boolean deletedFile;
    @JsonProperty("diff")
    String diff;
}
