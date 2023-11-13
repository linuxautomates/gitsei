package io.levelops.commons.databases.models.database.bullseye;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class BullseyeBuild {

    @JsonProperty("id")
    String id;

    @JsonProperty("cicd_job_run_id")
    String cicdJobRunId;

    @JsonProperty("build_id")
    String buildId;

    @JsonProperty("project")
    String jobName;

    @JsonProperty("built_at")
    Date builtAt;

    @JsonProperty("name")
    String name;

    @JsonProperty("file_hash")
    String fileHash;

    @JsonProperty("directory")
    String directory;

    @JsonProperty("functions_covered")
    Integer functionsCovered;

    @JsonProperty("total_functions")
    Integer totalFunctions;

    @JsonProperty("decisions_covered")
    Integer decisionsCovered;

    @JsonProperty("total_decisions")
    Integer totalDecisions;

    @JsonProperty("conditions_covered")
    Integer conditionsCovered;

    @JsonProperty("total_conditions")
    Integer totalConditions;

    @JsonProperty("source_files")
    List<BullseyeSourceFile> sourceFiles;

    @JsonProperty("folders")
    List<BullseyeFolder> folders;

    public static BullseyeBuild fromCodeCoverageReport(CodeCoverageReport report, UUID cicdJobId, String jobName, String fileHash) {
        return BullseyeBuild.builder()
                .cicdJobRunId(cicdJobId.toString())
                .buildId(report.getBuildId())
                .builtAt(getBuiltAt(report.getBuildId()))
                .jobName(jobName)
                .name(report.getName())
                .fileHash(fileHash)
                .directory(report.getDirectory())
                .functionsCovered(report.getFunctionsCovered())
                .totalFunctions(report.getTotalFunctions())
                .functionsCovered(report.getFunctionsCovered())
                .totalDecisions(report.getTotalDecisions())
                .decisionsCovered(report.getDecisionsCovered())
                .totalConditions(report.getTotalConditions())
                .conditionsCovered(report.getConditionsCovered())
                .folders(getFoldersFromFolderCoverages(report.getFolderCoverages()))
                .sourceFiles(getSourceFilesFromSourceFileCoverages(report.getSourceFiles()))
                .build();
    }

    private static List<BullseyeSourceFile> getSourceFilesFromSourceFileCoverages(List<SourceFileCoverage> sourceFileCoverages) {
        return CollectionUtils.emptyIfNull(sourceFileCoverages).stream()
                .map(sourceFile ->
                        BullseyeSourceFile.builder()
                                .name(sourceFile.getName())
                                .modificationTime(sourceFile.getModificationTime())
                                .totalFunctions(sourceFile.getTotalFunctions())
                                .functionsCovered(sourceFile.getFunctionsCovered())
                                .totalDecisions(sourceFile.getTotalDecisions())
                                .decisionsCovered(sourceFile.getDecisionsCovered())
                                .totalConditions(sourceFile.getTotalConditions())
                                .conditionsCovered(sourceFile.getConditionsCovered())
                                .build()
                ).collect(Collectors.toList());
    }

    private static List<BullseyeFolder> getFoldersFromFolderCoverages(List<FolderCoverage> folderCoverages) {
        return CollectionUtils.emptyIfNull(folderCoverages).stream()
                .map(folderCoverage ->
                        BullseyeFolder.builder()
                                .name(folderCoverage.getName())
                                .totalFunctions(folderCoverage.getTotalFunctions())
                                .functionsCovered(folderCoverage.getFunctionsCovered())
                                .totalDecisions(folderCoverage.getTotalDecisions())
                                .decisionsCovered(folderCoverage.getDecisionsCovered())
                                .totalConditions(folderCoverage.getTotalConditions())
                                .conditionsCovered(folderCoverage.getFunctionsCovered())
                                .sourceFiles(getSourceFilesFromSourceFileCoverages(folderCoverage.getSourceFiles()))
                                .folders(getFoldersFromFolderCoverages(folderCoverage.getFolderCoverages()))
                                .build()
                ).collect(Collectors.toList());
    }

    private static Date getBuiltAt(String buildId) {
        try {
            String[] tokens = buildId.split("_", 2);
            if (tokens.length != 2) {
                log.warn("Unable to parse date for Bullseye build " + buildId + ". Expected format is" +
                        "(<string without '_'>_yyyy-MM-dd_HH:mm:ss)");
                return null;
            }
            String dateTime = tokens[1];
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            return formatter.parse(dateTime);
        } catch (ParseException e) {
            log.error("Unable to parse date for Bullseye build " + buildId + ". Expected format is " +
                    "(<string without '_'>_yyyy-MM-dd_HH:mm:ss)");
            return null;
        }
    }
}
