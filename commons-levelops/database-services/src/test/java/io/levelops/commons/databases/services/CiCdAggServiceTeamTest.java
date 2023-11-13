package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.databases.models.database.CICDJobConfigChangeDTO;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.organization.TeamMemberId;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class CiCdAggServiceTeamTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private final String company = "test";
    private String teamId1;
    private static final boolean VALUES_ONLY = false;

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private UserService userService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    private CiCdAggsService ciCdAggsService;
    private IntegrationService integrationService;
    private TeamMembersDatabaseService teamMembersDatabaseService;
    private TeamsDatabaseService teamsDatabaseService;
    private UserIdentityService userIdentityService;
    private Integration integration;
    private CiCdAggsServiceTest ciCdAggsServiceTest;
    private ProductsDatabaseService productsDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        userService = new UserService(dataSource, MAPPER);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobConfigChangesDatabaseService = new CiCdJobConfigChangesDatabaseService(dataSource);
        ciCdAggsService = new CiCdAggsService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, MAPPER);
        teamsDatabaseService = new TeamsDatabaseService(dataSource, MAPPER);
        ciCdAggsServiceTest = new CiCdAggsServiceTest();
        userIdentityService = new UserIdentityService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        teamsDatabaseService.ensureTableExistence(company);
        integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jenkins")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration);
        productsDatabaseService = new ProductsDatabaseService(dataSource, MAPPER);
        productsDatabaseService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobConfigChangesDatabaseService.ensureTableExistence(company);
        ciCdAggsService.ensureTableExistence(company);
        var dbScmUsers = List.of(
                DbScmUser.builder()
                        .integrationId("1")
                        .displayName("meghana-levelops")
                        .originalDisplayName("meghana-levelops")
                        .cloudId("testread")
                        .build(),
                DbScmUser.builder()
                        .integrationId("1")
                        .displayName("sample-cog")
                        .originalDisplayName("sample-cog")
                        .cloudId("user-jenkins-7")
                        .build()
        );
        List<String> uuidsInserted = List.of(
                userIdentityService.upsert(company, dbScmUsers.get(0)),
                userIdentityService.upsert(company, dbScmUsers.get(1))
        );

        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(0).getDisplayName()).build(), UUID.fromString(uuidsInserted.get(0)));
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(1).getDisplayName()).build(), UUID.fromString(uuidsInserted.get(1)));

        Optional<TeamMemberId> teamMemberId1 = teamMembersDatabaseService.getId(
                company, UUID.fromString(uuidsInserted.get(0)));
        Optional<TeamMemberId> teamMemberId2 = teamMembersDatabaseService.getId(
                company, UUID.fromString(uuidsInserted.get(1)));
        UUID memberId1 = UUID.fromString(teamMemberId1.get().getTeamMemberId());
        UUID memberId2 = UUID.fromString(teamMemberId2.get().getTeamMemberId());
        DBTeam team1 = DBTeam.builder()
                .name("name")
                .description("description")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(memberId1).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(memberId1).build(),
                        DBTeam.TeamMemberId.builder().id(memberId2).build()))
                .build();
        teamId1 = teamsDatabaseService.insert(company, team1);
    }

    private void setupJobsAndJobRuns(UUID cicdInstanceId, List<CiCdAggsServiceTest.JobDetails> allJobDetails) throws SQLException {
        for(CiCdAggsServiceTest.JobDetails currentJobAllRuns : allJobDetails){
            CICDJob cicdJob = CICDJob.builder()
                    .cicdInstanceId(cicdInstanceId)
                    .jobName(currentJobAllRuns.getJobName())
                    .jobFullName(currentJobAllRuns.getJobFullName())
                    .jobNormalizedFullName(currentJobAllRuns.getJobFullName())
                    .branchName(currentJobAllRuns.getBranchName())
                    .projectName("project-1")
                    .moduleName(currentJobAllRuns.getModuleName())
                    .scmUrl(currentJobAllRuns.getScmUrl())
                    .scmUserId(currentJobAllRuns.getScmUserId())
                    .build();
            String cicdJobIdString = ciCdJobsDatabaseService.insert(company, cicdJob);
            Assert.assertNotNull(cicdJobIdString);
            UUID cicdJobId = UUID.fromString(cicdJobIdString);
            for(CiCdAggsServiceTest.JobRunDetails currentJobRun : currentJobAllRuns.getRuns()){
                CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                        .cicdJobId(cicdJobId)
                        .jobRunNumber(currentJobRun.getNumber())
                        .status(currentJobRun.getStatus())
                        .startTime(Instant.ofEpochSecond(currentJobRun.getStartTime()))
                        .duration(currentJobRun.getDuration().intValue())
                        .cicdUserId(currentJobRun.getUserId())
                        .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                        .referenceId(UUID.randomUUID().toString())
                        .scmCommitIds(currentJobRun.getCommitIds());
                if(CollectionUtils.isNotEmpty(currentJobRun.getParams())){
                    List<CICDJobRun.JobRunParam> params = new ArrayList<>();
                    for(CiCdAggsServiceTest.JobRunParam currentParam : currentJobRun.getParams()){
                        CICDJobRun.JobRunParam sanitized = CICDJobRun.JobRunParam.builder()
                                .type(currentParam.getType())
                                .name(currentParam.getName())
                                .value(currentParam.getValue())
                                .build();
                        params.add(sanitized);
                    }
                    bldr.params(params);
                }

                CICDJobRun cicdJobRun = bldr.build();
                ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
            }
        }
    }
    private long calculateOffset() {
        long diff = Instant.now().getEpochSecond() - 1593062362;
        long days = diff / (TimeUnit.DAYS.toSeconds(1));
        long offset = days * (TimeUnit.DAYS.toSeconds(1));
        return offset;
    }

    private List<CiCdAggsServiceTest.JobDetails> fixJobRunTimestamps(List<CiCdAggsServiceTest.JobDetails> allJobDetails, Long offset) {
        if(CollectionUtils.isEmpty(allJobDetails)) {
            return allJobDetails;
        }
        return allJobDetails.stream()
                .map(x -> {
                    if(CollectionUtils.isEmpty(x.getRuns())) {
                        return x;
                    }
                    x.setRuns(
                            x.getRuns().stream()
                                    .map(r -> {
                                        r.setStartTime(r.getStartTime() + offset);
                                        return r;
                                    })
                                    .collect(Collectors.toList())
                    );
                    return x;
                })
                .collect(Collectors.toList());
    }

    @Test
    public void testTeamsFilter() throws IOException, SQLException {
        String data = ResourceUtils.getResourceAsString("json/databases/jenkins_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, CiCdAggsServiceTest.JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        List<CICDInstance> cicdInstances = CiCdInstanceUtils.createCiCdInstances(ciCdInstancesDatabaseService, company,  integration, 2);
        List<CICDJob> cicdJobs = new ArrayList<>();
        List<CICDJob> cicdJobsWithoutInstances = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, null, 3);
        cicdJobs.addAll(cicdJobsWithoutInstances);
        for(int i=0; i<cicdInstances.size(); i++) {
            List<CICDJob> cicdJobsWithInstances = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, cicdInstances.get(i), 3);
            cicdJobs.addAll(cicdJobsWithInstances);
        }
        List<CICDJobConfigChange> cicdJobConfigChanges = new ArrayList<>();
        for(int i=0; i< cicdJobs.size(); i++){
            cicdJobConfigChanges.addAll(CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJobs.get(i), i, i+1));
        }

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(6);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.trend)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(6);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.cicd_job_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(6);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(6);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(3);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(3);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder().across(CiCdJobRunsFilter.DISTINCT.project_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(3);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(3);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(6);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(6);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobRuns(company,
                CiCdJobRunsFilter.builder()
                        .across(CiCdJobRunsFilter.DISTINCT.job_status)
                        .calculation(CiCdJobRunsFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY
        ).getTotalCount()).isEqualTo(3);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .across(CiCdJobConfigChangesFilter.DISTINCT.trend)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(8);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .across(CiCdJobConfigChangesFilter.DISTINCT.job_name)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .across(CiCdJobConfigChangesFilter.DISTINCT.instance_name)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .across(CiCdJobConfigChangesFilter.DISTINCT.qualified_job_name)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .across(CiCdJobConfigChangesFilter.DISTINCT.cicd_user_id)
                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),
                VALUES_ONLY,
                null
        ).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .build(),null,0, 100).getTotalCount()).isEqualTo(8);

        Assertions.assertThat(ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .jobNames(List.of("jobname-0"))
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .qualifiedJobNames(List.of( CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("jobname-0").build()))
                        .cicdUserIds(List.of("user-jenkins-6"))
                        .build(),null,0, 100).getTotalCount()).isEqualTo(7);

        Assertions.assertThat(ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .instanceNames(List.of("instance-name-1"))
                        .build(),null,0, 100).getTotalCount()).isEqualTo(8);

        Assertions.assertThat(ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .cicdUserIds(List.of("team_id:" +teamId1))
                        .jobNames(List.of("jobname-0"))
                        .build(), null, 0, 100).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testCiCdJobConfigExcludeFilters() throws IOException, SQLException {
        String data = ResourceUtils.getResourceAsString("json/databases/jenkins_plugin_result.json");
        List<CiCdAggsServiceTest.JobDetails> allJobDetails = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, CiCdAggsServiceTest.JobDetails.class));
        allJobDetails = fixJobRunTimestamps(allJobDetails, calculateOffset());
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        setupJobsAndJobRuns(null, allJobDetails);
        setupJobsAndJobRuns(cicdInstance.getId(), allJobDetails);
        List<CICDInstance> cicdInstances = CiCdInstanceUtils.createCiCdInstances(ciCdInstancesDatabaseService, company,  integration, 2);
        List<CICDJob> cicdJobs = new ArrayList<>();
        List<CICDJob> cicdJobsWithoutInstances = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, null, 3);
        cicdJobs.addAll(cicdJobsWithoutInstances);
        for(int i=0; i<cicdInstances.size(); i++) {
            List<CICDJob> cicdJobsWithInstances = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, cicdInstances.get(i), 3);
            cicdJobs.addAll(cicdJobsWithInstances);
        }
        List<CICDJobConfigChange> cicdJobConfigChanges = new ArrayList<>();
        for(int i=0; i< cicdJobs.size(); i++){
            cicdJobConfigChanges.addAll(CiCdJobConfigChangesUtils.createCICDJobConfigChanges(ciCdJobConfigChangesDatabaseService, company, cicdJobs.get(i), i, i+1));
        }

        DbListResponse<CICDJobConfigChangeDTO> dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .excludeJobNames(List.of(" Build-commons-levelops"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(45, dbListResponse.getCount().intValue());
        Assert.assertEquals(45, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(45, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .jobNames(List.of(" Build-commons-levelops"))
                        .excludeJobNames(List.of(" Build-commons-levelops"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .excludeProjects(List.of("project-0"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(33, dbListResponse.getCount().intValue());
        Assert.assertEquals(33, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(33, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .projects(List.of("project-0"))
                        .excludeProjects(List.of("project-0"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .excludeInstanceNames(List.of("instance-name-1"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(15, dbListResponse.getCount().intValue());
        Assert.assertEquals(15, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(15, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .instanceNames(List.of("instance-name-1"))
                        .excludeInstanceNames(List.of("instance-name-1"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(39, dbListResponse.getCount().intValue());
        Assert.assertEquals(39, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(39, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .types(List.of(CICD_TYPE.azure_devops))
                        .excludeTypes(List.of(CICD_TYPE.azure_devops))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .excludeCiCdUserIds(List.of("SCMTrigger"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(45, dbListResponse.getCount().intValue());
        Assert.assertEquals(45, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(45, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .cicdUserIds(List.of("SCMTrigger"))
                        .excludeCiCdUserIds(List.of("SCMTrigger"))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());

        CiCdJobQualifiedName ciCdJobQualifiedName=CiCdJobQualifiedName.builder().instanceName("instance-name-1").jobName("jobname-0").build();
        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .excludeQualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(11, dbListResponse.getCount().intValue());
        Assert.assertEquals(11, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(11, dbListResponse.getRecords().size());

        dbListResponse = ciCdAggsService.listCiCdJobConfigChanges(company,
                CiCdJobConfigChangesFilter.builder()
                        .qualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .excludeQualifiedJobNames(List.of(ciCdJobQualifiedName))
                        .build(), null, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(0, dbListResponse.getCount().intValue());
        Assert.assertEquals(0, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(0, dbListResponse.getRecords().size());
    }
}
