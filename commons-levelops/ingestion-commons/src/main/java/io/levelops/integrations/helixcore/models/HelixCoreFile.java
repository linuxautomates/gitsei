package io.levelops.integrations.helixcore.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixCoreFile.HelixCoreFileBuilder.class)
public class HelixCoreFile {

    private static final String ADDITION = "additions";
    private static final String DELETION = "deletions";
    private static final String CHANGE = "changes";

    @JsonProperty("depot_name")
    String depotName;

    @JsonProperty("change_list_id")
    int changeListId;

    @JsonProperty
    String name;

    @JsonProperty
    String SHA;

    @JsonProperty("commit_date")
    Date commitDate;

    @JsonProperty("depot_path_string")
    String depotPathString;

    @JsonProperty("branch_name")
    String branchName;

    @JsonProperty("file_type")
    String fileType;

    @JsonProperty("file_action")
    String fileAction;

    @JsonProperty("other_action")
    String otherAction;

    @JsonProperty("additions")
    int additions;

    @JsonProperty("deletions")
    int deletions;

    @JsonProperty("changes")
    int changes;
}
