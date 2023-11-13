package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.converters.DbBlackDuckConvertors;
import io.levelops.commons.databases.services.blackduck.BlackDuckDatabaseService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.blackduck.models.BlackDuckIssue;
import io.levelops.integrations.blackduck.models.BlackDuckProject;
import io.levelops.integrations.blackduck.models.BlackDuckVersion;
import io.levelops.integrations.blackduck.models.EnrichedProjectData;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class BlackDuckAggHelper {

    private static final String DATATYPE_PROJECTS = "projects";

    private final BlackDuckDatabaseService blackDuckDatabaseService;
    private final JobDtoParser jobDtoParser;

    public BlackDuckAggHelper(BlackDuckDatabaseService blackDuckDatabaseService, JobDtoParser jobDtoParser) {
        this.blackDuckDatabaseService = blackDuckDatabaseService;
        this.jobDtoParser = jobDtoParser;
    }

    public boolean setupProjects(String company, String integrationId,
                                 MultipleTriggerResults multipleTriggerResults) {
        return jobDtoParser.applyToResults(company, DATATYPE_PROJECTS, EnrichedProjectData.class,
                multipleTriggerResults.getTriggerResults().get(0),
                enrichedProjectData -> {
                    BlackDuckProject project = enrichedProjectData.getProject();
                    BlackDuckVersion version = enrichedProjectData.getVersion();
                    List<BlackDuckIssue> issues = enrichedProjectData.getIssues();
                    try {
                        String insertedProjectId = blackDuckDatabaseService.insert(company,
                                DbBlackDuckConvertors.fromProject(project, integrationId));
                        String versionId = setupVersions(company, version, insertedProjectId);
                        if (CollectionUtils.isNotEmpty(issues))
                            setupIssues(company, issues, versionId);
                    } catch (SQLException e) {
                        log.warn("BlackDuckAggHelper: error inserting entity: " + project.getName()
                                + " for version: " + version.getName(), e);
                    }
                },
                List.of());
    }

    public String setupVersions(String company, BlackDuckVersion version, String projectId) {
        return blackDuckDatabaseService.insertVersions(company,
                DbBlackDuckConvertors.fromVersion(version, projectId));
    }

    public void setupIssues(String company, List<BlackDuckIssue> issues, String versionId) {
        issues.forEach(issue -> blackDuckDatabaseService.insertIssues(company,
                DbBlackDuckConvertors.fromIssue(issue, versionId)));
    }

}
