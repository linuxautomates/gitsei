package io.levelops.commons.databases.models.database.tokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GerritRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GerritRepositoryServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static TagsService tagService;
    private static TagItemDBService tagItemDBService;
    private static ProductsDatabaseService productsDatabaseService;
    private static GerritRepositoryService repositoryService;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        productsDatabaseService = new ProductsDatabaseService(dataSource, m);
        tagService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);

        repositoryService = new GerritRepositoryService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        tagService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);

    }

    @Test
    public void testListWithProductIds() throws SQLException {
        List<String> ids = batchUpset();
        String uuidInserted = productsDatabaseService.insert(company, DBOrgProduct.builder()
                .name("Sample 1")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .name("github test")
                                .type("github")
                                .integrationId(1)
                                .filters(Map.of("integration_id", "1", "ids", ids))
                                .build()
                ))
                .build());
        DbListResponse<DbRepository> dbListResponse = repositoryService.listByFilter(company, "1", ids, null
                , 0, 10);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbRepository::getHtmlUrl).collect(Collectors.toList()))
                .contains("https://askdnasjdn");
        DbListResponse<DbRepository> dbListResponseWithProductIds = repositoryService.listByFilter(company, null, null,
                Set.of(UUID.fromString(uuidInserted))
                , 0, 10);
        Assertions.assertThat(dbListResponseWithProductIds.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(dbListResponseWithProductIds.getRecords().stream().map(DbRepository::getHtmlUrl).collect(Collectors.toList()))
                .contains("https://askdnasjdn");
    }

    private List<String> batchUpset() throws SQLException {
        List<DbRepository> repoList = new ArrayList<>();
        repoList.add(DbRepository.builder().name("asd").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("adf").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        repoList.add(DbRepository.builder().name("dfg").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("ad").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        repoList.add(DbRepository.builder().name("ggh").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("agd").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        repoList.add(DbRepository.builder().name("tyu").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("asd").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        repoList.add(DbRepository.builder().name("iop").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("ard").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        repoList.add(DbRepository.builder().name("apoi").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("fsd").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        repoList.add(DbRepository.builder().name("das").size(20)
                .isPrivate(false).ownerName("asd").ownerType("asd")
                .cloudId("afd").cloudCreatedAt(1L).cloudUpdatedAt(1L).cloudPushedAt(1L)
                .createdAt(1L).htmlUrl("https://askdnasjdn").integrationId("1")
                .languages(List.of()).masterBranch("some")
                .build());
        return repositoryService.batchUpsert("test", repoList);
    }
}
