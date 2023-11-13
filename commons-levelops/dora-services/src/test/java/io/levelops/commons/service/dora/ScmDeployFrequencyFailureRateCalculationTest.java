package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class ScmDeployFrequencyFailureRateCalculationTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static ScmDoraService scmDoraService;
    private static ScmDeployFrequencyFailureRateCalculation scmDeployFrequencyFailureRateCalculation;
    private VelocityConfigDTO velocityConfigDTO;
    private static String gitHubIntegrationId;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA IF NOT EXISTS " + company + ";").execute();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());

        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmDeployFrequencyFailureRateCalculation = new ScmDeployFrequencyFailureRateCalculation(dataSource, scmAggService);
        scmDoraService = new ScmDoraService(dataSource, scmAggService, scmDeployFrequencyFailureRateCalculation);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);

        String input = ResourceUtils.getResourceAsString("json/dora/github_pr.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        prs.getResponse().getRecords().forEach(repo -> repo.getPullRequests()
                .forEach(review -> {
                    try {
                        DbScmPullRequest tmp = DbScmPullRequest
                                .fromGithubPullRequest(review, repo.getId(), gitHubIntegrationId, null);
                        if ("branch4".equals(tmp.getSourceBranch())) {
                            tmp = tmp.toBuilder().targetBranch("hotfix").prClosedAt(null).build();
                        } else if ("branch3".equals(tmp.getSourceBranch())) {
                            tmp = tmp.toBuilder().targetBranch("hotfix").prMergedAt(null).merged(false).build();
                        }
                        scmAggService.insert(company, tmp);
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/dora/github_commit.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> repo.getEvents().stream()
                .filter(ev -> "PushEvent".equals(ev.getType()))
                .flatMap(ev -> ev.getCommits().stream())
                .forEach(commit -> {
                    DbScmCommit tmp = DbScmCommit
                            .fromGithubCommit(commit, repo.getId(), gitHubIntegrationId,
                                    commit.getGitCommitter().getDate().toInstant().getEpochSecond(), commit.getGitCommitter().getDate().toInstant().getEpochSecond());
                    if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                            tmp.getIntegrationId()).isEmpty()) {
                        try {
                            if (List.of("Commit 12", "Commit 13", "Commit 14", "Commit 15").contains(tmp.getMessage())) {
                                tmp = tmp.toBuilder().directMerge(true).build();
                            }
                            scmAggService.insertCommit(company, tmp);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        DbScmFile.fromGithubCommit(
                                        commit, repo.getId(), gitHubIntegrationId, currentTime.toInstant().getEpochSecond())
                                .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                    }
                }));

        input  = ResourceUtils.getResourceAsString("json/dora/github_tag.json");
        PaginatedResponse<GithubRepository> tags = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        tags.getResponse().getRecords().forEach(repo -> repo.getTags().forEach(tag -> {
            DbScmTag tmp = DbScmTag.fromGithubTag(tag, repo.getId(), gitHubIntegrationId);
            if (scmAggService.getTag(company, repo.getId(), repo.getId(), gitHubIntegrationId,
                    tmp.getTag()).isEmpty()) {
                try {
                    scmAggService.insertTag(company, tmp);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }));

        dataSource.getConnection().prepareStatement(ARRAY_UNIQ)
                .execute();
    }

    @Test
    public void testCalculateDFPrClosed() throws IOException, BadRequestException, IOException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("source_branch", Map.of("$begins", List.of("")));
        scmFilters.put("target_branch", Map.of("$begins", List.of("dev")));
        scmFilters.put("labels", Map.of("$begins", List.of("")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_closed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_closed)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        Integer actualCount = 27;
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_closed_at", SortingOrder.ASC),
                null,
                0,
                100,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_closed.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(27, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrClosedAt() != null));
    }

    @Test
    public void testCalculateDFPrMerged() throws IOException, BadRequestException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");

        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("source_branch", Map.of("$begins", List.of("branch1", "ien")));
        scmFilters.put("target_branch", Map.of("$begins", List.of("release", "main")));
        scmFilters.put("labels", Map.of("$begins", List.of("")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 6;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_merged_at", SortingOrder.ASC),
                null,
                0,
                100,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_merged.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(6, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrMergedAt() != null));
    }

    @Test
    public void testCalculateDFPrMergedClosedOnPrClosed() throws IOException, BadRequestException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");

        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("source_branch", Map.of("$begins", List.of("branch1", "ien")));
        scmFilters.put("target_branch", Map.of("$begins", List.of("dev", "main")));
        scmFilters.put("labels", Map.of("$begins", List.of("")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_closed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged_closed)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 18;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_closed_at", SortingOrder.DESC),
                null,
                0,
                100,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_closed.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(18, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrClosedAt() != null));
    }

    @Test
    public void testCalculateDFPrMergedClosedOnPrMerged() throws IOException, BadRequestException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");

        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("source_branch", Map.of("$begins", List.of("branch1", "ien")));
        scmFilters.put("target_branch", Map.of("$begins", List.of("dev", "main")));
        scmFilters.put("labels", Map.of("$begins", List.of("")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged_closed)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 18;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_merged_at", SortingOrder.ASC),
                null,
                0,
                100,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_merged.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(18, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrMergedAt() != null));
    }

    @Test
    public void testCalculateCFRPrClosed() throws IOException, BadRequestException, IOException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFiltersForTotal = new HashMap<>();
        scmFiltersForTotal.put("source_branch", Map.of("$begins", List.of("branch1", "ien")));
        scmFiltersForTotal.put("target_branch", Map.of("$begins", List.of("release", "main")));
        scmFiltersForTotal.put("labels", Map.of("$begins", List.of("")));

        Map<String, Map<String, List<String>>> scmFiltersForFail = new HashMap<>();
        scmFiltersForFail.put("source_branch", Map.of("$begins", List.of("branch3", "ien")));
        scmFiltersForFail.put("target_branch", Map.of("$begins", List.of("main")));
        scmFiltersForFail.put("labels", Map.of("$begins", List.of("")));

        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_closed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_closed)
                                        .scmFilters(scmFiltersForFail)
                                        .build())
                                .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_closed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_closed)
                                        .scmFilters(scmFiltersForTotal)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateChangeFailureRate(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        Integer actualCount = 3;
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_closed_at", SortingOrder.ASC),
                null,
                0,
                100,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_closed.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(3, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrClosedAt() != null));
    }

    @Test
    public void testCalculateCFRPrMerged() throws IOException, BadRequestException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFiltersForTotal = new HashMap<>();
        scmFiltersForTotal.put("source_branch", Map.of("$begins", List.of("branch1", "ien")));
        scmFiltersForTotal.put("target_branch", Map.of("$begins", List.of("release", "main")));
        scmFiltersForTotal.put("labels", Map.of("$begins", List.of("")));

        Map<String, Map<String, List<String>>> scmFiltersForFail = new HashMap<>();
        scmFiltersForFail.put("source_branch", Map.of("$begins", List.of("branch3", "ien")));
        scmFiltersForFail.put("target_branch", Map.of("$begins", List.of("main")));
        scmFiltersForFail.put("labels", Map.of("$begins", List.of("")));

        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .scmFilters(scmFiltersForFail)
                                        .build())
                                .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .scmFilters(scmFiltersForTotal)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateChangeFailureRate(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 2;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.MEDIUM);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prMergedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_merged_at", SortingOrder.DESC),
                null,
                0,
                100,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_merged.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(2, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrMergedAt() != null));
    }

    @Test
    public void testCalculateCFRPrMergedClosed() throws IOException, BadRequestException {

        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFiltersForTotal = new HashMap<>();
        scmFiltersForTotal.put("source_branch", Map.of("$begins", List.of("branch1", "ien")));
        scmFiltersForTotal.put("target_branch", Map.of("$begins", List.of("release", "main")));
        scmFiltersForTotal.put("labels", Map.of("$begins", List.of("")));

        Map<String, Map<String, List<String>>> scmFiltersForFail = new HashMap<>();
        scmFiltersForFail.put("source_branch", Map.of("$begins", List.of("branch3", "branch4", "ien")));
        scmFiltersForFail.put("target_branch", Map.of("$begins", List.of("main")));
        scmFiltersForFail.put("labels", Map.of("$begins", List.of("")));

        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_closed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged_closed)
                                        .scmFilters(scmFiltersForFail)
                                        .build())
                                .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged_closed)
                                        .scmFilters(scmFiltersForTotal)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateChangeFailureRate(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .prMergedRange(ImmutablePair.of(begin, end))
                        .build(),
                ScmCommitFilter.builder().integrationIds(List.of("1")).build(), null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 2;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.MEDIUM);

        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.failure_rate)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_closed_at", SortingOrder.ASC),
                null,
                0,
                100,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_merged_closed.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(2, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrClosedAt() != null));
    }

    @Test
    public void testGetPrBasedDrillDownData() throws IOException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("source_branch", Map.of("$begins", List.of("branch2")));
        scmFilters.put("target_branch", Map.of("$begins", List.of("main")));
        scmFilters.put("labels", Map.of("$begins", List.of("label2")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();
        Long begin = 1551332301L;
        Long end = new Date().toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond();
        DbListResponse<DbScmPullRequest> records = scmDoraService.getPrBasedDrillDownData(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .integrationIds(List.of("1"))
                        .calculation(ScmPrFilter.CALCULATION.deployment_frequency)
                        .prClosedRange(ImmutablePair.of(begin,end))
                        .build(),
                Map.of("pr_merged_at", SortingOrder.ASC),
                null,
                0,
                10,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                VelocityConfigDTO.DeploymentCriteria.pr_merged.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(2, (int) records.getRecords().size());
        Assert.assertTrue(records.getRecords().stream().allMatch(pr -> pr.getPrMergedAt() != null));
    }

    @Test
    public void testGetCommitBasedDrillDownData() throws IOException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("tags", Map.of("$begins", List.of("tag3")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_with_tag)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = currentTime.toInstant().getEpochSecond();
        DbListResponse<DbScmCommit> records = scmDoraService.getCommitBasedDrillDownData(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                0,
                10,
                Map.of("committed_at", SortingOrder.ASC),
                VelocityConfigDTO.DeploymentCriteria.commit_with_tag.toString(),
                VelocityConfigDTO.CalculationField.committed_at.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(1, (int) records.getTotalCount());
        Assert.assertTrue(records.getRecords().stream().allMatch(commit -> commit.getCommittedAt() != null));
    }

    @Test
    public void testDfForDirectMergedCommitWithTag() throws IOException, BadRequestException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("tags", Map.of("$begins", List.of("tag1", "tag2", "tag3", "tag4")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.commit_pushed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch_with_tag)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = currentTime.toInstant().getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder().integrationIds(List.of("1")).build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .commitPushedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 2;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmCommit> records = scmDoraService.getCommitBasedDrillDownData(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .commitPushedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                0,
                100,
                Map.of("commit_pushed_at", SortingOrder.DESC),
                VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch_with_tag.toString(),
                VelocityConfigDTO.CalculationField.commit_pushed_at.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(2, (int) records.getTotalCount());
        Assert.assertTrue(records.getRecords().stream().allMatch(commit -> commit.getCommitPushedAt() != null));
    }

    @Test
    public void testDfForCommitWithTag() throws IOException, BadRequestException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("tags", Map.of("$begins", List.of("tag1", "tag2", "tag3", "tag4")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_with_tag)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = currentTime.toInstant().getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder().integrationIds(List.of("1")).build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 4;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmCommit> records = scmDoraService.getCommitBasedDrillDownData(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                0,
                100,
                Map.of("committed_at", SortingOrder.DESC),
                VelocityConfigDTO.DeploymentCriteria.commit_with_tag.toString(),
                VelocityConfigDTO.CalculationField.committed_at.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(4, (int) records.getTotalCount());
        Assert.assertTrue(records.getRecords().stream().allMatch(commit -> commit.getCommittedAt() != null));
    }

    @Test
    public void testDfForCommitOnBranchWithoutTag() throws IOException, BadRequestException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);

        Map<String, Map<String, List<String>>> scmFilters = new HashMap<>();
        scmFilters.put("commit_branch", Map.of("$begins", List.of("branch1", "branch2", "branch4", "branch3", "main")));
        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch)
                                        .scmFilters(scmFilters)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = currentTime.toInstant().getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateDeploymentFrequency(company,
                ScmPrFilter.builder().integrationIds(List.of("1")).build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 4;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.LOW);

        DbListResponse<DbScmCommit> records = scmDoraService.getCommitBasedDrillDownData(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters(),
                0,
                100,
                Map.of("committed_at", SortingOrder.DESC),
                VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch.toString(),
                VelocityConfigDTO.CalculationField.committed_at.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(4, (int) records.getTotalCount());
        Assert.assertTrue(records.getRecords().stream().allMatch(commit -> commit.getCommittedAt() != null));
    }

    @Test
    public void testCFRForCommitOnBranchWithoutTag() throws IOException, BadRequestException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFiltersForTotal = new HashMap<>();
        scmFiltersForTotal.put("commit_branch", Map.of("$begins", List.of("main", "branch4")));
        scmFiltersForTotal.put("tags", Map.of("$begins", List.of("tag1", "tag2", "tag3", "tag4")));

        Map<String, Map<String, List<String>>> scmFiltersForFail = new HashMap<>();
        scmFiltersForFail.put("commit_branch", Map.of("$begins", List.of("branch4")));

        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch)
                                        .scmFilters(scmFiltersForFail)
                                        .build())
                                .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.commit_pushed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_with_tag)
                                        .scmFilters(scmFiltersForTotal)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = currentTime.toInstant().getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateChangeFailureRate(company,
                ScmPrFilter.builder().integrationIds(List.of("1")).build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .commitPushedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 1;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertEquals(doraResponseDTO.getStats().getBand(), DoraSingleStateDTO.Band.ELITE);

        DbListResponse<DbScmCommit> records = scmDoraService.getCommitBasedDrillDownData(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters(),
                0,
                100,
                Map.of("committed_at", SortingOrder.ASC),
                VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch.toString(),
                VelocityConfigDTO.CalculationField.committed_at.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(1, (int) records.getTotalCount());
        Assert.assertTrue(records.getRecords().stream().allMatch(commit -> commit.getCommittedAt() != null));
    }

    @Test
    public void testCFRForCommitWithTag() throws IOException, BadRequestException {
        String workFlowProfile = ResourceUtils.getResourceAsString("velocity/v1_workflow_profile_scm.json");
        velocityConfigDTO = m.readValue(workFlowProfile, VelocityConfigDTO.class);
        Map<String, Map<String, List<String>>> scmFiltersForTotal = new HashMap<>();
        scmFiltersForTotal.put("tags", Map.of("$begins", List.of("tag1", "tag2")));

        Map<String, Map<String, List<String>>> scmFiltersForFail = new HashMap<>();
        scmFiltersForFail.put("tags", Map.of("$begins", List.of("tag1", "tag2", "tag3", "tag4")));

        velocityConfigDTO = velocityConfigDTO.toBuilder()
                .changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder()
                        .isAbsoulte(true)
                        .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_with_tag)
                                        .scmFilters(scmFiltersForFail)
                                        .build())
                                .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.commit_pushed_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_with_tag)
                                        .scmFilters(scmFiltersForTotal)
                                        .build())
                                .build())
                        .build())
                .build();

        Long begin = 1551332301L;
        Long end = currentTime.toInstant().getEpochSecond();
        DoraResponseDTO doraResponseDTO = scmDoraService.calculateChangeFailureRate(company,
                ScmPrFilter.builder().integrationIds(List.of("1")).build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .commitPushedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO);
        DefaultObjectMapper.prettyPrint(doraResponseDTO);
        Integer actualCount = 4;
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getDay().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getWeek().size()>0);
        Assert.assertTrue(doraResponseDTO.getTimeSeries().getMonth().size()>0);
        Assert.assertEquals(doraResponseDTO.getTimeSeries().getMonth().stream().map(month -> month.getCount()).reduce(0, Integer::sum), actualCount);
        Assert.assertNull(doraResponseDTO.getStats().getBand());

        DbListResponse<DbScmCommit> records = scmDoraService.getCommitBasedDrillDownData(company,
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1"))
                        .committedAtRange(ImmutablePair.of(begin,end))
                        .build(),
                null,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters(),
                0,
                100,
                Map.of("committed_at", SortingOrder.ASC),
                VelocityConfigDTO.DeploymentCriteria.commit_with_tag.toString(),
                VelocityConfigDTO.CalculationField.committed_at.toString());
        records.getRecords().forEach(DefaultObjectMapper::prettyPrint);
        Assert.assertNotNull(records);
        Assert.assertEquals(4, (int) records.getTotalCount());
        Assert.assertTrue(records.getRecords().stream().allMatch(commit -> commit.getCommittedAt() != null));
    }
}
