package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

@Log4j2
public class ScmDoraLeadMTTRTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitlabIntegrationId;
    private static ScmDoraAggService scmDoraAggService;
    private static OrgUnitHelper unitsHelper;
    private static OrgUsersDatabaseService usersService;
    private static OrgUnitsDatabaseService unitsService;
    private static DBOrgUnit unit2;
    private static DBOrgUnit unit3;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgUnitCategory;
    private static OrgUnitCategory orgGroup1;

    private static IntegrationService integrationService;
    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static String orgGroupId1, orgGroupId;
    private static Pair<UUID, Integer> ids, ids2, ids3;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA IF NOT EXISTS " + company + ";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        new UserService(dataSource, m).ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        TagItemDBService tagItemService = new TagItemDBService(dataSource);

        scmAggService = new ScmAggService(dataSource, userIdentityService);
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        scmDoraAggService = new ScmDoraAggService(dataSource, scmAggService);
        TeamMembersDatabaseService teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        usersService = new OrgUsersDatabaseService(dataSource, m, versionsService, userIdentityService);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        dashboardWidgetService.ensureTableExistence(company);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,m);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,m,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, m);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,m);
        devProductivityProfileDbService.ensureTableExistence(company);

        unitsService = new OrgUnitsDatabaseService(dataSource, m, tagItemService, usersService, versionsService, dashboardWidgetService);
        unitsHelper = new OrgUnitHelper(unitsService, integrationService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        Integration integration = Integration.builder()
                .application("gitlab")
                .id("1")
                .name("gitlac test")
                .status("enabled")
                .build();
        gitlabIntegrationId = integrationService.insert(company, integration);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        versionsService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, m);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        usersService.ensureTableExistence(company);
        unitsService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/gitlab_prs_2021.json");
        PaginatedResponse<GitlabProject> gitlabPRs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        gitlabPRs.getResponse().getRecords().forEach(project -> project.getMergeRequests()
                .forEach(mergeRequest -> {
                    DbScmPullRequest gitlabMergeRequest = DbScmPullRequest.fromGitlabMergeRequest(
                            mergeRequest, project.getPathWithNamespace(), gitlabIntegrationId,null);
                    try {
                        if (scmAggService.getPr(company, gitlabMergeRequest.getId(),
                                        project.getId(), gitlabIntegrationId)
                                .isEmpty()) {
                            scmAggService.insert(company, gitlabMergeRequest);
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }));

        input = ResourceUtils.getResourceAsString("json/databases/gitlab_commits.json");
        PaginatedResponse<GitlabProject> response = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        response.getResponse().getRecords().forEach(project -> project.getCommits()
                .forEach(commit -> {
                    DbScmCommit gitlabCommit = DbScmCommit.fromGitlabCommit(
                            commit, project.getPathWithNamespace(), gitlabIntegrationId);
                    if (scmAggService.getCommit(
                                    company, gitlabCommit.getId(), project.getPathWithNamespace(), gitlabIntegrationId)
                            .isEmpty()) {
                        try {
                            scmAggService.insertCommit(company, gitlabCommit);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        DbScmFile.fromGitlabCommit(
                                        commit, project.getPathWithNamespace(), gitlabIntegrationId)
                                .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                    }
                }));
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();


        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("1").displayName("review-levelops").originalDisplayName("review-levelops").build())
                .reviewId("543339289").reviewer("ivan-levelops")
                .state("APPROVED").reviewedAt(300L)
                .build();

        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("2").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339290").reviewer("viraj-levelops")
                .state("COMMENTED").reviewedAt(600L)
                .build();

        DbScmPullRequest pr1 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("164")
                .integrationId(gitlabIntegrationId)

                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .targetBranch("fix-hf")
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr1);

        DbScmPullRequest pr2 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("167")
                .integrationId(gitlabIntegrationId)
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .sourceBranch("main-hf")
                .targetBranch("fix-hf-1")
                .prUpdatedAt(100L)
                .prMergedAt(1500L)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr2);

        DbScmPullRequest pr3 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/api-levelops")
                .number("167")
                .integrationId(gitlabIntegrationId)
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(true)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .prCreatedAt(100L)
                .targetBranch("LEV_XXX-hf")
                .sourceBranch("dev-hf")
                .commitShas(List.of("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2"))
                .prUpdatedAt(100L)
                .prMergedAt(1642490962L)
                .prClosedAt(1500L)
                .reviews(List.of(scmReview1))
                .build();
        scmAggService.insert(company, pr3);

        DbScmCommit commit1 = DbScmCommit.builder()
                .repoIds(List.of("levelops/devops-levelops")).integrationId(gitlabIntegrationId)
                .project("levelops/devops-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .commitUrl("url")
                .vcsType(VCS_TYPE.TFVC)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(DbScmUser.builder().integrationId(gitlabIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .committedAt(1642404562L)
                .createdAt(System.currentTimeMillis())
                .ingestedAt(System.currentTimeMillis())
                .build();
        scmAggService.insertCommit(company, commit1);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();

        DbScmTag tag1 = DbScmTag.builder()
                .commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .createdAt(Instant.now().getEpochSecond())
                .integrationId(gitlabIntegrationId)
                .repo("levelops/devops-levelops")
                .project("levelops/devops-levelops")
                .tag("example-hf")
                .updatedAt(Instant.now().getEpochSecond())
                .build();
        scmAggService.insertTag(company, tag1);

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("testread").username("cloudId").integrationType(integration.getApplication())
                        .integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(ScmDoraLeadMTTRTest.company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SCMTrigger").integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(ScmDoraLeadMTTRTest.company, orgUser2);
        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SYSTEM").username("cloudId").integrationType(integration.getApplication())
                        .integrationId(Integer.parseInt(gitlabIntegrationId)).build()))
                .versions(Set.of(1))
                .build();
        var userId3 = usersService.upsert(ScmDoraLeadMTTRTest.company, orgUser3);
        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
        DBOrgUnit unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integration.getId()))
                        .integrationFilters(Map.of("target_branches", List.of("fix-hf", "LEV_XXX-hf")))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .build();
        ids=unitsService.insertForId(ScmDoraLeadMTTRTest.company, unit1);
        unitsHelper.activateVersion(company,ids.getLeft());
        orgUnitCategory = OrgUnitCategory.builder()
                .name("TEAM B")
                .description("Sample team")
                .isPredefined(false)
                .build();
        orgGroupId = orgUnitCategoryDatabaseService.insert(company, orgUnitCategory);

        unit2 = DBOrgUnit.builder()
                .name("unit2")
                .description("My unit2")
                .active(true)
                .versions(Set.of(2))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integration.getId()))
                        .integrationFilters(Map.of("source_branches", List.of("main-hf", "dev-hf")))
                        .defaultSection(false)
                        .users(Set.of())
                        .build()))
                .refId(2)
                .build();
        ids2=unitsService.insertForId(ScmDoraLeadMTTRTest.company, unit2);
        unitsHelper.activateVersion(company,ids2.getLeft());
        unit3 = DBOrgUnit.builder()
                .name("unit3")
                .description("My unit3")
                .active(true)
                .versions(Set.of(2))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integration.getId()))
                        .integrationFilters(Map.of("approval_statuses", List.of("self approved", "peer approved")))
                        .defaultSection(false)
                        .users(Set.of(1, 3))
                        .build()))
                .refId(3)
                .build();
        ids3=unitsService.insertForId(ScmDoraLeadMTTRTest.company, unit3);
        unitsHelper.activateVersion(company,ids3.getLeft());
    }


    @Test
    public void testGenerateLeadTimeMTTRReport() throws SQLException, BadRequestException {

        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                .builder()
                .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes)
                .across(ScmPrFilter.DISTINCT.creator)
                .build(), null, VelocityConfigDTO.builder()
                .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                        .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                Map.of("$contains", List.of("hf"))))
                        .build())
                .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getLeadTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("ELITE");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.tags,
                                        Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getLeadTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("ELITE");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .projects(List.of("ctlo2020/test-velocity-cog"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .projects(List.of("levelops/commons-levelops"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .targetBranches(List.of("fix-hf", "LEV_XXX-hf"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getLeadTime()).isNotZero();
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("ELITE");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .sourceBranches(List.of("trends-test", "lev-1983", "lev-1983"))
                        .targetBranches(List.of("fix-hf", "LEV_XXX-hf"))
                        .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();

    }

    @Test
    public void testMeanTimeToRecover() throws SQLException, BadRequestException {
        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .across(ScmPrFilter.DISTINCT.none)
                        .calculation(ScmPrFilter.CALCULATION.mean_time_to_recover).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.tags,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.target_branch, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getRecoverTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("HIGH");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .integrationIds(List.of("1"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .prUpdatedRange(ImmutablePair.of(0L, 9999999999L))
                        .calculation(ScmPrFilter.CALCULATION.mean_time_to_recover).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getRecoverTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("HIGH");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .integrationIds(List.of("1"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .prUpdatedRange(ImmutablePair.of(0L, 9999999999L))
                        .calculation(ScmPrFilter.CALCULATION.mean_time_to_recover).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.tags,
                                        Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getRecoverTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("HIGH");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .integrationIds(List.of("1"))
                        .across(ScmPrFilter.DISTINCT.none)
                        .prUpdatedRange(ImmutablePair.of(0L, 9999999999L))
                        .calculation(ScmPrFilter.CALCULATION.mean_time_to_recover).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$ends", List.of("wrong")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("qrong"))))
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.tags,
                                        Map.of("$ends", List.of("wrong"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);


        Optional<DBOrgUnit> dbOrgUnit1 = unitsService.get(company, 2, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(2)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit1.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company,
                ScmPrFilter.fromDefaultListRequest(defaultListRequest, ScmPrFilter.DISTINCT.none
                        , ScmPrFilter.CALCULATION.mean_time_to_recover), ouConfig,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getRecoverTime()).isNotZero();
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("HIGH");

        dbOrgUnit1 = unitsService.get(company, 2, true);
        defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(2)).ouExclusions(List.of("approval_statuses")).build();
        ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit1.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company,
                ScmPrFilter.fromDefaultListRequest(defaultListRequest, ScmPrFilter.DISTINCT.none
                        , ScmPrFilter.CALCULATION.mean_time_to_recover), ouConfig,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch,
                                        Map.of("$begins", List.of("dev")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getRecoverTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("HIGH");
    }

    @Test
    public void testOuConfig() throws SQLException, BadRequestException {
        Optional<DBOrgUnit> dbOrgUnit1 = unitsService.get(company, 1, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(1)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getCICDIntegrationTypes(), defaultListRequest,
                dbOrgUnit1.orElseThrow(), false);
        defaultListRequest = ouConfig.getRequest();
        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company,
                ScmPrFilter.fromDefaultListRequest(defaultListRequest, ScmPrFilter.DISTINCT.none
                        , ScmPrFilter.CALCULATION.lead_time_for_changes), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf")),
                                        VelocityConfigDTO.ScmConfig.Field.labels, Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getLeadTime()).isNotZero();
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("ELITE");
    }

    @Test
    public void testAcross() throws SQLException, BadRequestException {
        DbListResponse<DbAggregationResult> dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .across(ScmPrFilter.DISTINCT.creator)
                        .calculation(ScmPrFilter.CALCULATION.mean_time_to_recover).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getRecoverTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getAdditionalKey()).isEqualTo("viraj-levelops");
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("HIGH");

        dbAggregationResult = scmDoraAggService.generateLeadTimeAndMTTRReport(company, ScmPrFilter
                        .builder()
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .calculation(ScmPrFilter.CALCULATION.lead_time_for_changes).build(), null,
                VelocityConfigDTO.builder()
                        .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                                .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch,
                                        Map.of("$contains", List.of("hf"))))
                                .build())
                        .build());
        Assertions.assertThat(dbAggregationResult).isNotNull();
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getLeadTime()).isEqualTo(86400L);
        Assertions.assertThat(dbAggregationResult.getCount()).isEqualTo(1);
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(dbAggregationResult.getRecords().stream().findFirst().get().getBand()).isEqualTo("ELITE");
    }

}
