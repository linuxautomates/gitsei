package io.levelops.commons.databases.services.dev_productivity.es.handlers;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.dev_productivity.handlers.PercentageOfReworkHandler;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.scm.EsScmCommit;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.faceted_search.services.scm_service.EsScmCommitsService;
import io.levelops.faceted_search.services.scm_service.EsTestUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.PERCENTAGE_OF_REWORK;
import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class EsPercentageOfReworkHandlerTest {

    private static ESContext esContext;
    private static EsTestUtils esTestUtils = new EsTestUtils();
    private static EsScmCommitsService esScmCommitsService;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String index = "scm_commits_test";
    public static String company = "test";

    private static final Integer SECTION_ORDER = 0;
    private static PercentageOfReworkHandler percentageOfReworkHandler;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    private static OrgVersionsDatabaseService versionsService;
    private static OrgUsersDatabaseService usersService;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @BeforeClass
    public static void startESCreateClient() throws IOException, InterruptedException, SQLException {

        esContext = esTestUtils.initializeESClient();
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement(ARRAY_UNIQ)
                .execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmAggService.ensureTableExistence(company);
        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);
        usersService = new OrgUsersDatabaseService(dataSource, m, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);

        EsTestUtils.insertIntegration(dataSource, Integration.builder()
                .id("1861")
                .application("github-1")
                .name("github test-1")
                .status("enabled")
                .build());

        EsTestUtils.insertIntegration(dataSource, Integration.builder()
                .id("2228")
                .application("github-2")
                .name("github test-2")
                .status("enabled")
                .build());

        EsTestUtils.insertIntegration(dataSource, Integration.builder()
                .id("1815")
                .application("github-3")
                .name("github test-3")
                .status("enabled")
                .build());

        EsTestUtils.createIndex(esContext.getClient(), index, "index/commits_index_template.json");
        insertData();

        ESClientFactory esClientFactory = EsTestUtils.buildESClientFactory(esContext);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        esScmCommitsService = new EsScmCommitsService(esClientFactory, dataSource, userIdentityService);
        percentageOfReworkHandler = new PercentageOfReworkHandler(scmAggService, esScmCommitsService, null);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    }

    @Test
    public void test() throws SQLException, IOException {

        UUID orgUserId = UUID.randomUUID();

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        String sql = "select id from test.integration_users where cloud_id = 'ctlo2020' and integration_id = '2228' ";
        UUID userId = template.queryForObject(sql, Map.of(), UUID.class);

        IntegrationUserDetails esUser = IntegrationUserDetails.builder()
                .cloudId("ctlo2020")
                .integrationId(2228)
                .integrationType(IntegrationType.GITHUB)
                .integrationUserId(UUID.fromString("ab986f2d-6f05-4653-89b8-7e094ebf966c"))
                .displayName("ctlo2020")
                .build();

        IntegrationUserDetails dbUser = IntegrationUserDetails.builder()
                .cloudId("ctlo2020")
                .integrationId(1815)
                .integrationType(IntegrationType.GITHUB)
                .integrationUserId(userId)
                .displayName("ctlo2020")
                .build();

        OrgUserDetails orgUserDetails = OrgUserDetails.builder()
                .orgUserId(orgUserId)
                .email("user@levelops.io")
                .fullName("User Name")
                .IntegrationUserDetailsList(List.of(esUser, dbUser))
                .build();


        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .featureType(PERCENTAGE_OF_REWORK).maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75).build();
        ImmutablePair<Long, Long> committedRange = ImmutablePair.of(0L, Instant.now().getEpochSecond());

        DevProductivityFilter filter = DevProductivityFilter.builder()
                .timeRange(committedRange).build();
        DevProductivityFilter esFilter = DevProductivityFilter.builder()
                .timeRange(committedRange).forceSource("es").build();

        FeatureResponse dbFeatureResponse = percentageOfReworkHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), filter, orgUserDetails, Map.of(), TenantSCMSettings.builder().legacyUpdateIntervalConfig(1652781958l).build());
        FeatureResponse esFeatureResponse = percentageOfReworkHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), esFilter, orgUserDetails, Map.of(), TenantSCMSettings.builder().legacyUpdateIntervalConfig(1652781958l).build());

        Assert.assertNotNull(dbFeatureResponse);
        Assert.assertNotNull(esFeatureResponse);

        Assertions.assertThat(dbFeatureResponse.getFeatureUnit()).isEqualTo(esFeatureResponse.getFeatureUnit());
        Assertions.assertThat(dbFeatureResponse.getMean()).isEqualTo(esFeatureResponse.getMean());
        Assertions.assertThat(dbFeatureResponse.getScore()).isEqualTo(esFeatureResponse.getScore());
        Assertions.assertThat(dbFeatureResponse.getResult()).isEqualTo(esFeatureResponse.getResult());
        Assertions.assertThat(dbFeatureResponse.getRating()).isEqualTo(esFeatureResponse.getRating());

        FeatureBreakDown dbBreakDown = percentageOfReworkHandler.getBreakDown(company, feature, Map.of(), filter, orgUserDetails, Map.of(), TenantSCMSettings.builder().legacyUpdateIntervalConfig(1652781958l).build(), Map.of(), 0, 100);
        FeatureBreakDown esBreakDown = percentageOfReworkHandler.getBreakDown(company, feature, Map.of(), esFilter, orgUserDetails, Map.of(), TenantSCMSettings.builder().legacyUpdateIntervalConfig(1652781958l).build(), Map.of(), 0, 100);

        Assert.assertNotNull(dbBreakDown);
        Assert.assertNotNull(esBreakDown);
        Assertions.assertThat(dbBreakDown.getCount()).isEqualTo(esBreakDown.getCount());

    }

    private static void insertData() throws IOException, SQLException {

        String data = ResourceUtils.getResourceAsString("data/commits.json");
        List<EsScmCommit> allScmCommits = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, EsScmCommit.class));
        List<DbScmCommit> allScmDbCommits = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, DbScmCommit.class));

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        for (DbScmCommit commit : allScmDbCommits) {

            List<DbScmFileCommit> fileCommitList = commit.getFileCommitList().stream()
                    .map(f -> DbScmFileCommit.builder()
                            .commitSha(f.getCommitSha())
                            .fileId(f.getFileId())
                            .change(f.getChange())
                            .addition(f.getAddition())
                            .deletion(f.getDeletion())
                            .previousCommittedAt(f.getPreviousCommittedAt() != null ? TimeUnit.MILLISECONDS.toSeconds(f.getPreviousCommittedAt()) : null)
                            .committedAt(TimeUnit.MILLISECONDS.toSeconds(f.getCommittedAt()))
                            .createdAt(TimeUnit.MILLISECONDS.toSeconds(f.getCreatedAt()))
                            .build()).collect(Collectors.toList());

            List<DbScmFile> fileList = commit.getFileCommitList().stream()
                    .map(f -> DbScmFile.builder()
                            .id(f.getFileId())
                            .filename(f.getFileName())
                            .filetype(f.getFileType())
                            .commitShas(List.of(f.getCommitSha()))
                            .integrationId(f.getIntegrationId())
                            .repoId(f.getRepo())
                            .project(f.getProject())
                            .createdAt(TimeUnit.MILLISECONDS.toSeconds(f.getCreatedAt()))
                            .build()).collect(Collectors.toList());

            commit = commit.toBuilder()
                    .committedAt(TimeUnit.MILLISECONDS.toSeconds(commit.getCommittedAt()))
                    .build();

            scmAggService.insertCommit(company, commit);

            fileList.forEach(f -> {

                DbScmFile finalF = f;
                List<DbScmFileCommit> list = fileCommitList.stream().filter(fc -> fc.getFileId().equals(finalF.getId())).collect(Collectors.toList());
                f = f.toBuilder()
                        .fileCommits(list)
                        .build();
                insertFile(company, f, template);
            });
        }

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (EsScmCommit commit : allScmCommits) {
            BulkOperation.Builder b = new BulkOperation.Builder();
            b.update(v -> v
                    .index(index)
                    .id(commit.getId())
                    .action(a -> a
                            .docAsUpsert(true)
                            .doc(commit)
                    )
            );
            bulkOperations.add(b.build());
        }
        BulkRequest.Builder bldr = new BulkRequest.Builder();
        bldr.operations(bulkOperations);
        esContext.getClient().bulk(bldr.build());
    }

    public static String insertFile(String company, DbScmFile file, NamedParameterJdbcTemplate template) {

        String fileId = template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String fileSql = "INSERT INTO " + company + ".scm_files" +
                    " (repo_id,project,integration_id,filename, filetype) VALUES(?,?,?,?,?)" +
                    " ON CONFLICT (filename,integration_id,repo_id,project)" +
                    " DO NOTHING";

            try (PreparedStatement insertFilePstmt = conn.prepareStatement(
                    fileSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertFilePstmt.setObject(i++, file.getRepoId());
                insertFilePstmt.setObject(i++, file.getProject());
                insertFilePstmt.setObject(i++, NumberUtils.toInt(file.getIntegrationId()));
                insertFilePstmt.setObject(i++, file.getFilename());
                insertFilePstmt.setObject(i, file.getFiletype());

                int insertedRows = insertFilePstmt.executeUpdate();
                String insertedRowId = null;

                if (insertedRows == 0) {
                    return null;
                }
                try (ResultSet rs = insertFilePstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        insertedRowId = rs.getString(1);
                    }
                }
                if (insertedRowId == null) {
                    throw new SQLException("Failed to get inserted rowid.");
                }
                return insertedRowId;
            }
        }));
        final String returnVal = fileId;

        String fileCommitSql = " INSERT INTO " + company + ".scm_file_commits"
                + " (file_id,commit_sha,change,addition,deletion,committed_at,previous_committed_at)"
                + " VALUES(:file_id, :commit_sha, :change, :addition, :deletion, :committed_at, :previous_committed_at) ON CONFLICT (file_id,commit_sha)"
                + " DO NOTHING;";

        for (DbScmFileCommit fileCommit : file.getFileCommits()) {
            Map<String, Object> param = new HashMap<>();
            param.put("file_id", UUID.fromString(returnVal));
            param.put("commit_sha", fileCommit.getCommitSha());
            param.put("change", fileCommit.getChange());
            param.put("addition", fileCommit.getAddition());
            param.put("deletion", fileCommit.getDeletion());
            param.put("committed_at", LocalDateTime.ofEpochSecond(fileCommit.getCommittedAt(), 0, ZoneOffset.UTC));
            param.put("previous_committed_at", fileCommit.getPreviousCommittedAt() != null ? LocalDateTime.ofEpochSecond(fileCommit.getPreviousCommittedAt(), 0, ZoneOffset.UTC) : null);

            template.update(fileCommitSql, param);
        }
        return returnVal;
    }

    @AfterClass
    public static void closeResources() throws Exception {
        esTestUtils.deleteIndex(index);
        esTestUtils.closeResources();
    }
}
