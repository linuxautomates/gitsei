package io.levelops.commons.databases.models.database.sonarqube;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.sonarqube.models.Analyse;
import io.levelops.integrations.sonarqube.models.Project;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSonarQubeProject.DbSonarQubeProjectBuilder.class)
public class DbSonarQubeProject {

    private static final String UNASSIGNED = "_UNASSIGNED_";

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("organization")
    String organization;

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("visibility")
    String visibility;

    @JsonProperty("last_analysis_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXX")
    Date lastAnalysisDate;

    @JsonProperty("revision")
    String revision;

    @JsonProperty("branches")
    List<DbSonarQubeBranch> branches;

    @JsonProperty("pullRequests")
    List<DbSonarQubePullRequest> pullRequests;

    @JsonProperty("analyses")
    List<Analyse> analyses;

    @JsonProperty("measures")
    List<DbSonarQubeMeasure> measures;

    public static DbSonarQubeProject fromComponent(@NonNull Project project, String integrationId, Date ingestedAt) {
        Date truncatedDate = DateUtils.truncate(ingestedAt, Calendar.DATE);
        return DbSonarQubeProject.builder()
                .integrationId(integrationId)
                .organization(project.getOrganization())
                .lastAnalysisDate(project.getLastAnalysisDate())
                .key(project.getKey())
                .name(project.getName())
                .visibility(project.getVisibility())
                .revision(project.getRevision())
                .branches(CollectionUtils.emptyIfNull(project.getBranches()).stream()
                        .map(branch -> DbSonarQubeBranch.fromBranch(branch, ingestedAt))
                        .collect(Collectors.toList()))
                .pullRequests(CollectionUtils.emptyIfNull(project.getPullRequests()).stream()
                        .map(pr -> DbSonarQubePullRequest.fromPullRequest(pr, truncatedDate))
                        .collect(Collectors.toList()))
                .measures(CollectionUtils.emptyIfNull(project.getMeasures()).stream()
                        .map(measure -> DbSonarQubeMeasure.fromMeasure(measure, truncatedDate))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .analyses(project.getAnalyses())
                .build();
    }
}
