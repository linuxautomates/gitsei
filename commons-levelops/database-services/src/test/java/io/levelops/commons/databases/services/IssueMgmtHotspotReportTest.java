package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.Repository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Log4j2
public class IssueMgmtHotspotReportTest {
    private static final String company = "test26";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static WorkItemsService workItemsService;
    private static WorkItemTimelineService workItemTimelineService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static ScmIssueMgmtService scmIssueMgmtService;
    private static GitRepositoryService repositoryService;
    private static IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    private static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private static String azureDevopsIntegrationId;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        scmIssueMgmtService = new ScmIssueMgmtService(dataSource, scmAggService, null, workItemFieldsMetaService);
        workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemsService = new WorkItemsService(dataSource, null, null,
                null, null, null,
                null, null, workItemsPrioritySLAService, null, null,
                workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        workItemTimelineService = new WorkItemTimelineService(dataSource);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);

        repositoryService = new GitRepositoryService(dataSource);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        azureDevopsIntegrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("azure test")
                .status("enabled")
                .build());
        scmAggService.ensureTableExistence(company);
        workItemsService.ensureTableExistence(company);
        workItemTimelineService.ensureTableExistence(company);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);

        Long truncatedDate = io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE);
        String input = ResourceUtils.getResourceAsString("json/databases/issue_mgmt_commits.json");
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        enrichedProjectDataResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    Repository repository = enrichedProjectData.getRepository();
                    List<Commit> commits = enrichedProjectData.getCommits();
                    log.info("setupAzureDevopsCommits company {}, integrationId {}, projectId {}, repo {}, no of commits {}",
                            company, azureDevopsIntegrationId, project.getId(), repository.getName(), commits.size());
                    for (Commit commit : commits) {
                        DbScmCommit dbScmCommit = DbScmCommit.fromAzureDevopsCommit(commit,
                                project, repository.getName(), azureDevopsIntegrationId, truncatedDate);
                        log.info("setupAzureDevopsCommits company {}, integrationId {}, projectId {}, repo {}, commit {}," +
                                        " workitem ids {}, jira keys {}", company, azureDevopsIntegrationId, project.getId(),
                                repository.getName(), dbScmCommit.getCommitSha(), dbScmCommit.getWorkitemIds(), dbScmCommit.getIssueKeys());
                        String rowId = null;
                        try {
                            rowId = scmAggService.insertCommit(company, dbScmCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        log.info("setupAzureDevopsCommits company {}, integrationId {}, projectId {}, repo {}, commit {}," +
                                        " workitem ids {}, jira keys {}, row id {}", company, azureDevopsIntegrationId, project.getId(),
                                repository.getName(), dbScmCommit.getCommitSha(), dbScmCommit.getWorkitemIds(), dbScmCommit.getIssueKeys(), rowId);
                        DbScmFile.fromAzureDevopsCommitFiles(project, commit,
                                repository.getName(), azureDevopsIntegrationId).forEach(file -> {
                            scmAggService.insertFile(company, file);
                        });
                    }
                }
        );

        String workItemsResource = "json/databases/azure_devops_work_items_3.json";
        String timelinesResource = "json/databases/azure_devops_workitem_history_3.json";
        String iterationsResource = "json/databases/azure_devops_iterations_2.json";
        IssueMgmtTestUtil.setup(company, azureDevopsIntegrationId, workItemsService, workItemTimelineService,
                issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime, timelinesResource,
                iterationsResource, workItemsResource, null, null, null,userIdentityService);
    }

    @Test
    public void testScmIssueMgmtFiles() {
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder().build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000)).isNotNull();
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .filename("/sample.txt")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .module("/sample.txt")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .filename("unknown")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .module("unknown")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .repoIds(List.of("pr-demo-repo"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .repoIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .integrationIds(List.of("1"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .integrationIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeProjects(List.of("project-test-11"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeProjects(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .projects(List.of("project-test-11"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .projects(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeRepoIds(List.of("pr-demo-repo"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeRepoIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(4);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmFiles(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeRepoIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder()
                        .workItemIds(List.of("1", "6"))
                        .build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null, 0, 10000).getTotalCount()).isEqualTo(3);

    }

    @Test
    public void testScmIssueMgmtModules() {
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null)).isNotNull();
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .repoIds(List.of("pr-demo-repo"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .repoIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .repoIds(List.of("unknown", "pr-demo-repo"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .projects(List.of("project-test-11"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .projects(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .integrationIds(List.of("1"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .integrationIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeRepoIds(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeRepoIds(List.of("pr-demo-repo"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeProjects(List.of("project-test-11"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .excludeProjects(List.of("unknown"))
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .filename("/sample.txt")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .filename("/readme1.txt")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .filename("unknown")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .module("/sample.txt")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .module("/readme1.txt")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .module("unknown")
                        .build(),
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmIssueMgmtService.listIssueMgmtScmModules(company,
                ScmFilesFilter.builder()
                        .listFiles(true)
                        .build(),
                WorkItemsFilter.builder()
                        .workItemIds(List.of("5"))
                        .build(), WorkItemsMilestoneFilter.builder().build(),
                Map.of(), null).getTotalCount()).isEqualTo(0);
    }
}
