package io.levelops.integrations.helixcore.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.perforce.p4java.core.file.IFileSpec;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixCoreFile.HelixCoreFileBuilder.class)
public class HelixCoreFile {

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

    public static HelixCoreFile fromIFileSpec(IFileSpec iFileSpec) {
        final Pattern DEPOT_PATH = Pattern.compile(".*/(.*)", Pattern.CASE_INSENSITIVE);
        String fileName = "_UNKNOWN_";
        if (iFileSpec.getDepotPathString() != null) {
            fileName = iFileSpec.getDepotPathString();
            Matcher depotMatcher = DEPOT_PATH.matcher(iFileSpec.getDepotPathString());
            if (depotMatcher.find()) {
                fileName = depotMatcher.group(1);
            }
        }
        return HelixCoreFile.builder()
                .depotName(iFileSpec.getRepoName())
                .changeListId(iFileSpec.getChangelistId())
                .name(fileName)
                .SHA(iFileSpec.getSha())
                .commitDate(iFileSpec.getDate())
                .depotPathString(iFileSpec.getDepotPathString())
                .fileType(iFileSpec.getFileType())
                .fileAction(iFileSpec.getOtherAction() != null ? iFileSpec.getOtherAction().toString() : null)
                .build();
    }
}
