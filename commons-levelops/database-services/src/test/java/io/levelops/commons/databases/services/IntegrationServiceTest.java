package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.exceptions.FunctionWithException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.services.IntegrationService.UNLINK_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IntegrationServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    static String companyName = "test";

    static IntegrationService integrationService;
    static TagItemDBService tagItemDBService;
    static TagsService tagsService;

    static JdbcTemplate template;

    @BeforeClass
    public static void beforeClass() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        integrationService = new IntegrationService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        tagsService = new TagsService(dataSource);
        template = integrationService.template.getJdbcTemplate();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        template.execute(
                "DROP SCHEMA IF EXISTS " + companyName + " CASCADE;" +
                        "CREATE SCHEMA " + companyName + ";"
        );
        System.out.println(template.queryForObject("SELECT current_database();", String.class));

        integrationService.ensureTableExistence(companyName);
        tagsService.ensureTableExistence(companyName);
        tagItemDBService.ensureTableExistence(companyName);
    }

    @Before
    public void setUp() throws Exception {
        template.execute(
                "DELETE from " + companyName + ".tags"
        );
    }

    private String insert(String name) throws SQLException {
        Integration integration = Integration.builder()
                .name(name)
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .metadata(Map.of("test", "dummy-value"))
                .satellite(true)
                .build();

        return integrationService.insert(companyName, integration);
    }

    @Test
    public void test() throws SQLException {
        String id = insert("my jira");
        String tagId = tagsService.insert(companyName, Tag.builder().name("database").build());
        tagItemDBService.batchInsert(companyName, List.of(TagItemMapping.builder()
                .itemId(id)
                .tagId(tagId)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));

        Optional<Integration> output = integrationService.get(companyName, id);
        assertThat(output).isPresent();
        assertThat(output.get().getName()).isEqualTo("my jira");
        assertThat(output.get().getApplication()).isEqualTo("jira");
        assertThat(output.get().getUrl()).isEqualTo("http");
        assertThat(output.get().getDescription()).isEqualTo("desc");
        assertThat(output.get().getSatellite()).isTrue();
        assertThat(output.get().getId()).isEqualTo(id);
        assertThat(output.get().getTags()).isEqualTo(List.of(tagId));
        assertThat(output.get().getAuthentication()).isEqualTo(Integration.Authentication.UNKNOWN);

        assertThat(integrationService.get(companyName, "99999999")).isNotPresent();

        integrationService.delete(companyName, id);

        assertThat(integrationService.get(companyName, id)).isNotPresent();
    }

    @Test
    public void linkedCredentials() throws SQLException {
        String id1 = insert("parent");
        String id2 = integrationService.insert(companyName, Integration.builder()
                .name("child1")
                .url("http")
                .status("good")
                .application("jira")
                .linkedCredentials(id1)
                .build());

        assertThat(integrationService.get(companyName, id1)).isPresent();
        assertThat(integrationService.get(companyName, id2)).isPresent();
        assertThat(integrationService.get(companyName, id2).orElseThrow().getLinkedCredentials()).isEqualTo(id1);

        // -- filter
        assertThat(integrationService.listByFilter(companyName, null, false, null, null, null, null, null, null, id1, 0, 10).getRecords().stream().map(Integration::getId)).containsExactly(id2);
        assertThat(integrationService.listByFilter(companyName, null, false, null, null, null, null, null, null, id2, 0, 10).getRecords().stream().map(Integration::getId)).isEmpty();

        // -- invalid insert
        assertThatThrownBy(() -> integrationService.insert(companyName, Integration.builder()
                .name("child2")
                .url("http")
                .status("good")
                .application("jira")
                .linkedCredentials("9999")
                .build())).hasMessageContaining("insert or update on table \"integrations\" violates foreign key constraint");

        // -- invalid delete
        assertThatThrownBy(() -> integrationService.delete(companyName, id1)).hasMessageContaining("update or delete on table \"integrations\" violates foreign key constraint");


        // --- update
        String id3 = insert("child2");
        assertThat(integrationService.get(companyName, id3)).isPresent();
        assertThat(integrationService.get(companyName, id3).orElseThrow().getLinkedCredentials()).isNull();

        FunctionWithException<String, String, SQLException> testUpdate = (String linkedCredentials) -> {
            integrationService.update(companyName, Integration.builder().id(id3).linkedCredentials(linkedCredentials).build());
            return integrationService.get(companyName, id3).orElseThrow().getLinkedCredentials();
        };

        assertThat(testUpdate.apply(id1)).isEqualTo(id1);
        assertThat(testUpdate.apply(id2)).isEqualTo(id2);
        assertThat(testUpdate.apply(UNLINK_CREDENTIALS)).isNull();
        assertThatThrownBy(() -> testUpdate.apply("9999")).hasMessageContaining("insert or update on table \"integrations\" violates foreign key constraint");

        // --- delete
        integrationService.delete(companyName, id3);
        assertThat(integrationService.get(companyName, id3)).isEmpty();
        integrationService.delete(companyName, id2);
        assertThat(integrationService.get(companyName, id2)).isEmpty();
        integrationService.delete(companyName, id1);
        assertThat(integrationService.get(companyName, id1)).isEmpty();
    }

    @Test
    public void update() throws SQLException {
        String id = insert("my jira");
        integrationService.update(companyName, Integration.builder()
                .id(id)
                .name("new name")
                .url("newurl")
                .status("new status")
                .application("new app") // wont work
                .description("new desc")
                .appendMetadata(true)
                .metadata(Map.of("repo", "dummy-levelops"))
                .satellite(false)
                .authentication(Integration.Authentication.OAUTH)
                .build());

        Optional<Integration> output = integrationService.get(companyName, id);
        assertThat(output).isPresent();
        assertThat(output.get().getName()).isEqualTo("new name");
        assertThat(output.get().getApplication()).isEqualTo("jira");
        assertThat(output.get().getUrl()).isEqualTo("newurl");
        assertThat(output.get().getDescription()).isEqualTo("new desc");
        assertThat(output.get().getSatellite()).isFalse();
        assertThat(output.get().getId()).isEqualTo(id);
        assertThat(output.get().getAuthentication()).isEqualTo(Integration.Authentication.OAUTH);
        System.out.println(output.get().getMetadata());
        assertThat(output.get().getMetadata()).isEqualTo(Map.of("repo", "dummy-levelops", "test", "dummy-value"));
    }

    @Test
    public void filter() throws SQLException {
        String id1 = insert("a");
        String id2 = insert("b");
        String id3 = insert("c");
        String id4 = insert("repo_config_integ");
        integrationService.insertConfig(companyName, IntegrationConfig.builder()
                .integrationId(id1)
                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                .build());
        integrationService.insertConfig(companyName, IntegrationConfig.builder().integrationId(id2)
                .customHygieneList(List.of(IntegrationConfig.CustomHygieneEntry.builder().name("232323").build())).build());
        integrationService.insertConfig(companyName, IntegrationConfig.builder()
                .integrationId(id4)
                .repoConfig(List.of(
                        IntegrationConfig.RepoConfigEntry.builder()
                                .repoId("repo_1")
                                .pathPrefix("/FirstDepot")
                                .build(),
                        IntegrationConfig.RepoConfigEntry.builder()
                                .repoId("repo_2")
                                .pathPrefix("/SecondDepot")
                                .build()
                )).build());
        DbListResponse<Integration> list;
        String tagId1 = tagsService.insert(companyName, Tag.builder().name("database1").build());
        tagItemDBService.batchInsert(companyName, List.of(TagItemMapping.builder()
                .itemId(id1)
                .tagId(tagId1)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));
        String tagId2 = tagsService.insert(companyName, Tag.builder().name("database2").build());
        tagItemDBService.batchInsert(companyName, List.of(TagItemMapping.builder()
                .itemId(id2)
                .tagId(tagId2)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));
        tagItemDBService.batchInsert(companyName, List.of(
                TagItemMapping.builder()
                        .itemId(id3)
                        .tagId(tagId1)
                        .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                        .build(),
                TagItemMapping.builder()
                        .itemId(id3)
                        .tagId(tagId2)
                        .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                        .build()));

        list = integrationService.listByFilter(companyName, null, List.of("jira"), null, null, null, 0, 10);
        assertThat(list.getRecords()).hasSize(4);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id1, id2, id3, id4);
        assertThat(list.getRecords().stream().map(Integration::getName)).containsExactlyInAnyOrder("a", "b", "c", "repo_config_integ");
        assertThat(integrationService.stream(companyName, null, false, List.of("jira"), null, null, null, null, null, null).map(Integration::getId))
                .containsExactlyInAnyOrder(id1, id2, id3, id4);

        list = integrationService.listByFilter(companyName, null, List.of("jira"), null, null,
                List.of(Integer.valueOf(tagId1)), 0, 10);
        assertThat(list.getRecords()).hasSize(2);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id1, id3);
        assertThat(list.getRecords().stream().map(Integration::getName)).containsExactlyInAnyOrder("a", "c");
        assertThat(integrationService.stream(companyName, null, false, List.of("jira"), null, null, List.of(Integer.valueOf(tagId1)), null, null, null).map(Integration::getId))
                .containsExactlyInAnyOrder(id1, id3);

        list = integrationService.listByFilter(companyName, null, null, false, null, null, 0, 10);
        assertThat(list.getRecords()).hasSize(0);
        list = integrationService.listByFilter(companyName, null, null, true, null, null, 0, 10);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id1, id2, id3, id4);

        list = integrationService.listByFilter(companyName, null, null, true, List.of(Integer.parseInt(id1), Integer.parseInt(id2), Integer.parseInt(id3)), null, 0, 10);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id1, id2, id3);

        list = integrationService.listByFilter(companyName, null, null, null, null, null, 0, 10);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id1, id2, id3, id4);

        list = integrationService.listByFilter(companyName, null, null, null, null, null, 0, 2);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id4, id3);

        list = integrationService.listByFilter(companyName, null, null, null, null, null, 1, 2);
        assertThat(list.getRecords().stream().map(Integration::getId)).containsExactlyInAnyOrder(id2, id1);

        DbListResponse<IntegrationConfig> configs = integrationService.listConfigs(companyName, List.of(id3), 0, 10);
        assertThat(configs.getRecords().size()).isEqualTo(0);

        configs = integrationService.listConfigs(companyName, List.of(id1, id2), 0, 10);
        assertThat(configs.getRecords().size()).isEqualTo(2);

        configs = integrationService.listConfigs(companyName, List.of(id1), 0, 10);
        assertThat(configs.getRecords().size()).isEqualTo(1);
        assertThat(configs.getRecords().get(0).getMetadata().getConfigUpdatedAt()).isEqualTo(123L);

        configs = integrationService.listConfigs(companyName, List.of(id2), 0, 10);
        assertThat(configs.getRecords().size()).isEqualTo(1);
        assertThat(configs.getRecords().get(0).getCustomHygieneList().size()).isEqualTo(1);
        assertThat(configs.getRecords().get(0).getCustomHygieneList().get(0).getId()).isEqualTo(null);
        assertThat(configs.getRecords().get(0).getCustomHygieneList().get(0).getName()).isEqualTo("232323");
        assertThat(configs.getRecords().get(0).getCustomHygieneList().get(0).getMissingFields()).isEqualTo(null);
        assertThat(configs.getRecords().get(0).getMetadata()).isNotNull();
        assertThat(configs.getRecords().get(0).getMetadata().getConfigUpdatedAt()).isNull();

        configs = integrationService.listConfigs(companyName, List.of(id4), 0, 10);
        assertThat(configs.getRecords().size()).isEqualTo(1);
        assertThat(configs.getRecords().stream().map(IntegrationConfig::getIntegrationId)).containsExactly(id4);
        assertThat(configs.getRecords().get(0).getRepoConfig().size()).isEqualTo(2);
        assertThat(configs.getRecords().get(0).getRepoConfig().get(0).getRepoId()).isEqualTo("repo_1");
        assertThat(configs.getRecords().get(0).getRepoConfig().get(0).getPathPrefix()).isEqualTo("/FirstDepot");
        assertThat(configs.getRecords().get(0).getRepoConfig().get(1).getRepoId()).isEqualTo("repo_2");
        assertThat(configs.getRecords().get(0).getRepoConfig().get(1).getPathPrefix()).isEqualTo("/SecondDepot");
    }

    @Test
    public void testBulkDelete() throws SQLException {
        String id1 = insert("my jira");
        String tagId1 = tagsService.insert(companyName, Tag.builder().name("database1").build());
        tagItemDBService.batchInsert(companyName, List.of(TagItemMapping.builder()
                .itemId(id1)
                .tagId(tagId1)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));
        String id2 = insert("my zendesk");
        String tagId2 = tagsService.insert(companyName, Tag.builder().name("database2").build());
        tagItemDBService.batchInsert(companyName, List.of(TagItemMapping.builder()
                .itemId(id2)
                .tagId(tagId2)
                .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                .build()));
        integrationService.bulkDelete(companyName, List.of(id1, id2));
        assertThat(integrationService.get(companyName, id1)).isEmpty();
        assertThat(tagItemDBService.get(companyName, id1)).isEmpty();
        assertThat(integrationService.get(companyName, id2)).isEmpty();
        assertThat(tagItemDBService.get(companyName, id2)).isEmpty();
    }

}