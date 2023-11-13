package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.Repository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

@Log4j2
public class FileCommitsTest {
    private static final String company = "test55";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String azureDevopsIntegrationId;
    private static UserIdentityService userIdentityService;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        azureDevopsIntegrationId = "1";
        azureDevopsIntegrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("azure test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);

        currentTime = new Date();
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
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }


    @Test
    public void test() throws SQLException {
        DbListResponse<DbScmFile> filesList = scmAggService.list(company, ScmFilesFilter.builder().build(), Map.of(), 0, 10000);
        for (DbScmFile file : filesList.getRecords()) {
            List<DbScmFileCommit> fileCommits = scmAggService.getFileCommits(company, file.getId());
            if (fileCommits.size() > 1 && !fileCommits.get(1).getCommittedAt().equals(fileCommits.get(0).getCommittedAt())) {
                Assertions.assertThat(fileCommits.get(1).getPreviousCommittedAt()).isEqualTo(fileCommits.get(0).getCommittedAt());
            }
        }
    }

    @Test
    public void testCommitAggregate() {
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .build(), null).getRecords().get(0).getLinesAddedCount()).isEqualTo(5);
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .build(), null).getRecords().get(0).getLinesChangedCount()).isEqualTo(12);
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .build(), null).getRecords().get(0).getLinesRemovedCount()).isEqualTo(1);

        var res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .legacyCodeConfig(1629015199L)
                        .build(), null);
        DefaultObjectMapper.prettyPrint(res);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("project-test-11");
        Assertions.assertThat(res.getRecords().get(0).getPctLegacyRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctNewLines()).isEqualTo(44.44);
        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .legacyCodeConfig(1609828549L)
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("pr-demo-repo");
        Assertions.assertThat(res.getRecords().get(0).getPctLegacyRefactoredLines()).isEqualTo(0);
        Assertions.assertThat(res.getRecords().get(0).getPctRefactoredLines()).isEqualTo(55.56);
        Assertions.assertThat(res.getRecords().get(0).getPctNewLines()).isEqualTo(44.44);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .daysOfWeek(List.of("Thursday"))
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .excludeDaysOfWeek(List.of("Thursday"))
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .legacyCodeConfig(1627891999L)
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(res.getRecords().get(0).getAdditionalKey()).isEqualTo("srinath.chandrashekhar");
        Assertions.assertThat(res.getRecords().get(0).getPctLegacyRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctNewLines()).isEqualTo(44.44);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .legacyCodeConfig(1630039341L)
                        .aggInterval(AGG_INTERVAL.month)
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(3);
        Assertions.assertThat(res.getRecords().get(0).getAdditionalKey()).isEqualTo("6-2021");
        Assertions.assertThat(res.getRecords().get(0).getPctLegacyRefactoredLines()).isEqualTo(0);
        Assertions.assertThat(res.getRecords().get(0).getPctRefactoredLines()).isEqualTo(0);
        Assertions.assertThat(res.getRecords().get(0).getPctNewLines()).isEqualTo(100.0);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.code_change)
                        .legacyCodeConfig(1629015199L)
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("small");
        Assertions.assertThat(res.getRecords().get(0).getPctLegacyRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctNewLines()).isEqualTo(44.44);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.code_change)
                        .legacyCodeConfig(1629015199L)
                        .codeChangeUnit("files")
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("small");
        Assertions.assertThat(res.getRecords().get(0).getPctLegacyRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctRefactoredLines()).isEqualTo(27.78);
        Assertions.assertThat(res.getRecords().get(0).getPctNewLines()).isEqualTo(44.44);
        Assertions.assertThat(res.getRecords().get(0).getAvgChangeSize()).isEqualTo(1.125f);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.code_change)
                        .codeChangeSize(Map.of("$gt", "10", "$lt", "50"))
                        .legacyCodeConfig(1629015199L)
                        .codeChangeUnit("lines")
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(0);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .codeChanges(List.of("small"))
                        .legacyCodeConfig(1629015199L)
                        .codeChangeUnit("lines")
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .codeChanges(List.of("medium"))
                        .codeChangeSizeConfig(Map.of("small", "0", "medium", "10"))
                        .legacyCodeConfig(1629015199L)
                        .codeChangeUnit("lines")
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .codeChanges(List.of("large"))
                        .codeChangeSizeConfig(Map.of("small", "1", "medium", "2"))
                        .legacyCodeConfig(1629015199L)
                        .codeChangeUnit("files")
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);

        res = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .codeChanges(List.of("small"))
                        .legacyCodeConfig(1629015199L)
                        .codeChangeUnit("files")
                        .build(), null);
        Assertions.assertThat(res.getRecords().size()).isEqualTo(1);
    }

     @Test
    public void testCommitsList(){
        var listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .repoIds(List.of("pr-demo-repo"))
                        .legacyCodeConfig(1622621599L)
                        .build(),
                Map.of(), null, 0, 10);
        DefaultObjectMapper.prettyPrint(listRes);
        Assertions.assertThat(listRes.getRecords().size()).isNotEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesAdded()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesChanged()).isEqualTo(1);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesRemoved()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getPctLegacyLines()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getPctNewLines()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getPctRefactoredLines()).isEqualTo(100);

        listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .commitShas(List.of("1f9a98e0c31a3b1e6768dee75ef51f57c3b3e167"))
                        .legacyCodeConfig(1622621599L)
                        .build(),
                Map.of(), null, 0, 10);
        Assertions.assertThat(listRes.getRecords().size()).isNotEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesAdded()).isEqualTo(2);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesChanged()).isEqualTo(1);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesRemoved()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getPctLegacyLines()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getPctNewLines()).isEqualTo(100);
        Assertions.assertThat(listRes.getRecords().get(0).getPctRefactoredLines()).isEqualTo(0);

        listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .excludeDaysOfWeek(List.of("Sunday"))
                        .build(),
                Map.of(), null,0, 100);
        DefaultObjectMapper.prettyPrint(listRes);
        Assertions.assertThat(listRes.getRecords().size()).isEqualTo(16);

        listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .excludeDaysOfWeek(List.of("Sunday"))
                        .daysOfWeek(List.of("Thursday"))
                        .build(),
                Map.of(), null,0, 100);
        DefaultObjectMapper.prettyPrint(listRes);
        Assertions.assertThat(listRes.getRecords().size()).isEqualTo(8);

        listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .daysOfWeek(List.of("Thursday"))
                        .build(),
                Map.of(), null,0, 100);
        DefaultObjectMapper.prettyPrint(listRes);
        Assertions.assertThat(listRes.getRecords().size()).isEqualTo(8);

        listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .commitShas(List.of("1f9a98e0c31a3b1e6768dee75ef51f57c3b3e167"))
                        .codeChanges(List.of("medium"))
                        .codeChangeSizeConfig(Map.of("small", "1", "medium", "10"))
                        .legacyCodeConfig(1622621599L)
                        .build(),
                Map.of(), null, 0, 10);
        DefaultObjectMapper.prettyPrint(listRes);
        Assertions.assertThat(listRes.getRecords().size()).isNotEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesAdded()).isEqualTo(2);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesChanged()).isEqualTo(1);

        listRes = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .repoIds(List.of("pr-demo-repo"))
                        .codeChanges(List.of("small"))
                        .legacyCodeConfig(1622621599L)
                        .build(),
                Map.of(), null, 0, 10);
        Assertions.assertThat(listRes.getRecords().size()).isNotEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesAdded()).isEqualTo(0);
        Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesChanged()).isEqualTo(1);

         listRes = scmAggService.listCommits(
                 company,
                 ScmCommitFilter.builder()
                         .codeChanges(List.of("small"))
                         .ignoreFilesJoin(false)
                         .build(),
                 Map.of(), null, 0, 10);

         Assertions.assertThat(listRes.getTotalCount()).isEqualTo(16);
         Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesAdded()).isEqualTo(0);
         Assertions.assertThat(listRes.getRecords().get(0).getTotalLinesChanged()).isEqualTo(1);

         listRes = scmAggService.listCommits(
                 company,
                 ScmCommitFilter.builder()
                         .returnHasIssueKeys(true)
                         .build(),
                 Map.of(), null, 0, 10);
         Assertions.assertThat(listRes.getRecords().get(0).getHasIssueKeys()).isNotNull();
         Assertions.assertThat(listRes.getRecords().get(0).getHasIssueKeys()).isFalse();
         Assertions.assertThat(listRes.getRecords().get(1).getHasIssueKeys()).isTrue();
         Assertions.assertThat(listRes.getRecords().get(2).getHasIssueKeys()).isFalse();


         listRes = scmAggService.listCommits(
                 company,
                 ScmCommitFilter.builder()
                         .returnHasIssueKeys(false)
                         .build(),
                 Map.of(), null, 0, 10);
         Assertions.assertThat(listRes.getRecords().get(0).getHasIssueKeys()).isNull();


         listRes = scmAggService.listCommits(
                 company,
                 ScmCommitFilter.builder()
                         .build(),
                 Map.of(), null, 0, 10);
         Assertions.assertThat(listRes.getRecords().get(0).getHasIssueKeys()).isNull();
    }
}
