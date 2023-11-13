package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuild;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuildBatch;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.services.AWSDevToolsBuildBatchDatabaseService;
import io.levelops.commons.databases.services.AWSDevToolsBuildDatabaseService;
import io.levelops.commons.databases.services.AWSDevToolsProjectDatabaseService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.awsdevtools.models.CBBuild;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
import io.levelops.integrations.awsdevtools.models.CBProject;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
public class AWSDevToolsAggHelper {

    private static final String DATATYPE_PROJECTS = "projects";
    private static final String DATATYPE_BUILDS = "builds";
    private static final String DATATYPE_BUILD_BATCHES = "build_batches";

    private final JobDtoParser jobDtoParser;
    private final AWSDevToolsProjectDatabaseService awsDevToolsProjectDatabaseService;
    private final AWSDevToolsBuildDatabaseService awsDevToolsBuildDatabaseService;
    private final AWSDevToolsBuildBatchDatabaseService awsDevToolsBuildBatchDatabaseService;

    @Autowired
    public AWSDevToolsAggHelper(JobDtoParser jobDtoParser,
                                AWSDevToolsProjectDatabaseService awsDevToolsProjectDatabaseService,
                                AWSDevToolsBuildDatabaseService awsDevToolsBuildDatabaseService,
                                AWSDevToolsBuildBatchDatabaseService awsDevToolsBuildBatchDatabaseService) {
        this.jobDtoParser = jobDtoParser;
        this.awsDevToolsProjectDatabaseService = awsDevToolsProjectDatabaseService;
        this.awsDevToolsBuildDatabaseService = awsDevToolsBuildDatabaseService;
        this.awsDevToolsBuildBatchDatabaseService = awsDevToolsBuildBatchDatabaseService;
    }

    public boolean setupProjects(String customer,
                                 String integrationId,
                                 MultipleTriggerResults triggerResults) {
        return jobDtoParser.applyToResults(customer, DATATYPE_PROJECTS, CBProject.class,
                triggerResults.getTriggerResults().get(0),
                project -> {
                    try {
                        DbAWSDevToolsProject dbProject = DbAWSDevToolsProject.fromProject(project, integrationId);
                        awsDevToolsProjectDatabaseService.insertAndReturnId(customer, dbProject);
                    } catch (Exception e) {
                        log.error("setupProjects: error inserting project with name: " +
                                project.getProject().getName(), e);
                    }
                },
                List.of());
    }

    public boolean setupBuilds(String customer,
                               String integrationId,
                               MultipleTriggerResults triggerResults) {
        return jobDtoParser.applyToResults(customer, DATATYPE_BUILDS, CBBuild.class,
                triggerResults.getTriggerResults().get(0),
                build -> {
                    try {
                        DbAWSDevToolsBuild dbBuild = DbAWSDevToolsBuild.fromBuild(build, integrationId);
                        awsDevToolsBuildDatabaseService.insertAndReturnId(customer, dbBuild);
                    } catch (Exception e) {
                        log.error("setupBuilds: error inserting build with id: " +
                                build.getBuild().getId(), e);
                    }
                },
                List.of());
    }

    public boolean setupBuildBatches(String customer,
                                     String integrationId,
                                     MultipleTriggerResults triggerResults) {
        return jobDtoParser.applyToResults(customer, DATATYPE_BUILD_BATCHES, CBBuildBatch.class,
                triggerResults.getTriggerResults().get(0),
                buildBatch -> {
                    try {
                        DbAWSDevToolsBuildBatch dbBuildBatch = DbAWSDevToolsBuildBatch.fromBuildBatch(buildBatch, integrationId);
                        awsDevToolsBuildBatchDatabaseService.insertAndReturnId(customer, dbBuildBatch);
                    } catch (Exception e) {
                        log.error("setupBuildBatches: error inserting build batch with id: "
                                + buildBatch.getBuildBatch().getBuildBatchNumber(), e);
                    }
                },
                List.of());
    }
}
