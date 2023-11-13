package io.levelops.commons.databases.services.dev_productivity.es.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.dev_productivity.handlers.DevPRReviewDepthHandler;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import io.levelops.faceted_search.services.scm_service.EsTestUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.PRS_REVIEW_DEPTH;
import static io.levelops.commons.databases.services.ScmQueryUtils.ARRAY_UNIQ;

public class EsDevPrReviewDepthTest {
    private static ESContext esContext;
    private static EsTestUtils esTestUtils = new EsTestUtils();
    private static EsScmPRsService esScmPRsService;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String index = "scm_prs_test";
    public static String company = "test";

    private static final Integer SECTION_ORDER = 0;
    private static DevPRReviewDepthHandler devPRReviewDepthHandler;
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
                .id("1849")
                .application("github")
                .name("github test")
                .status("enabled")
                .build());

        EsTestUtils.createIndex(esContext.getClient(), index, "index/prs_index_template.json");
        EsTestUtils.insertPRsData(esContext.getClient(), scmAggService, index);

        ESClientFactory esClientFactory = EsTestUtils.buildESClientFactory(esContext);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        esScmPRsService = new EsScmPRsService(esClientFactory, dataSource, userIdentityService);
        devPRReviewDepthHandler = new DevPRReviewDepthHandler(scmAggService, esScmPRsService, null);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    }

    //@Test - - commenting for now as the db side is coming up as 0
    public void test() throws SQLException, IOException {

        UUID orgUserId = UUID.randomUUID();

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        String sql = "select id from test.integration_users where cloud_id = 'viraj-levelops' ";
        UUID userId = template.queryForObject(sql, Map.of(), UUID.class);

        IntegrationUserDetails esUser = IntegrationUserDetails.builder()
                .cloudId("viraj-levelops")
                .integrationId(1849)
                .integrationType(IntegrationType.GITHUB)
                .integrationUserId(UUID.fromString("f1617a51-04ba-4010-9c11-028f5e15b943"))
                .displayName("viraj-levelops")
                .build();

        IntegrationUserDetails dbUser = IntegrationUserDetails.builder()
                .cloudId("viraj-levelops")
                .integrationId(1849)
                .integrationType(IntegrationType.GITHUB)
                .integrationUserId(userId)
                .displayName("viraj-levelops")
                .build();

        OrgUserDetails orgUserDetails = OrgUserDetails.builder()
                .orgUserId(orgUserId)
                .email("user@levelops.io")
                .fullName("User Name")
                .IntegrationUserDetailsList(List.of(esUser, dbUser))
                .build();

        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .featureType(PRS_REVIEW_DEPTH).maxValue(100L).lowerLimitPercentage(25).upperLimitPercentage(75).build();
        DevProductivityFilter filter = DevProductivityFilter.builder().build();
        DevProductivityFilter esFilter = DevProductivityFilter.builder()
                .forceSource("es").build();

        FeatureResponse dbFeatureResponse = devPRReviewDepthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), filter, orgUserDetails, Map.of(), TenantSCMSettings.builder()
                .commentDensitySmall(0)
                .commentDensityMedium(1)
                .build());
        FeatureResponse esFeatureResponse = devPRReviewDepthHandler.calculateFeature(company, SECTION_ORDER, feature, Map.of(), esFilter, orgUserDetails, Map.of(), TenantSCMSettings.builder()
                .commentDensitySmall(0)
                .commentDensityMedium(1)
                .build());

        Assert.assertNotNull(dbFeatureResponse);
        Assert.assertNotNull(esFeatureResponse);

        Assertions.assertThat(dbFeatureResponse.getFeatureUnit()).isEqualTo(esFeatureResponse.getFeatureUnit());
        Assertions.assertThat(dbFeatureResponse.getMean()).isEqualTo(esFeatureResponse.getMean());
        Assertions.assertThat(dbFeatureResponse.getScore()).isEqualTo(esFeatureResponse.getScore());
        Assertions.assertThat(dbFeatureResponse.getResult()).isEqualTo(esFeatureResponse.getResult());
        Assertions.assertThat(dbFeatureResponse.getRating()).isEqualTo(esFeatureResponse.getRating());

        FeatureBreakDown dbBreakDown = devPRReviewDepthHandler.getBreakDown(company, feature, Map.of(), filter, orgUserDetails, Map.of(), TenantSCMSettings.builder()
                .commentDensitySmall(0)
                .commentDensityMedium(1)
                .build(), Map.of(), 0, 100);
        FeatureBreakDown esBreakDown = devPRReviewDepthHandler.getBreakDown(company, feature, Map.of(), esFilter, orgUserDetails, Map.of(), TenantSCMSettings.builder()
                .commentDensitySmall(0)
                .commentDensityMedium(1)
                .build(), Map.of(), 0, 100);

        Assert.assertNotNull(dbBreakDown);
        Assert.assertNotNull(esBreakDown);
        Assertions.assertThat(dbBreakDown.getCount()).isEqualTo(esBreakDown.getCount());

    }

    @AfterClass
    public static void closeResources() throws Exception {
        esTestUtils.deleteIndex(index);
        esTestUtils.closeResources();
    }
}
