package io.levelops.commons.databases.services.atlassian_connect;

import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectEvent;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.atlassian_connect.AtlassianConnectDatabaseService.AtlassianConnectAppMetadataFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

public class AtlassianConnectDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    static DataSource dataSource;
    private static DatabaseSchemaService databaseSchemaService;

    private static AtlassianConnectDatabaseService service;

    private AtlassianConnectAppMetadata createMetadataWithDefaultValues() {
        return AtlassianConnectAppMetadata.builder()
                .atlassianClientKey("atlassianClientKey")
                .installedAppKey("installedAppKey")
                .atlassianBaseUrl("atlassianBaseUrl")
                .atlassianDisplayUrl("atlassianDisplayUrl")
                .productType("productType")
                .description("description")
                .events(List.of(
                        AtlassianConnectEvent.builder()
                                .eventType("install")
                                .timestamp(Instant.now())
                                .build()))
                .atlassianUserAccountId("atlassianUserAccountId")
                .otp("1234")
                .enabled(true)
                .build();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        databaseSchemaService = new DatabaseSchemaService(dataSource);
        databaseSchemaService.ensureSchemaExistence(DatabaseService.SchemaType.ATLASSIAN_CONNECT_SCHEMA.getSchemaName());
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        service = new AtlassianConnectDatabaseService(dataSource, DefaultObjectMapper.get());
        service.ensureTableExistence("");
    }

    @Before
    public void cleanUp() {
        String sql = "DELETE FROM " + DatabaseService.SchemaType.ATLASSIAN_CONNECT_SCHEMA.getSchemaName()  + ".atlassian_connect_metadata";
        new JdbcTemplate(dataSource).execute(sql);
    }

    private void assertCompare(AtlassianConnectAppMetadata m1, AtlassianConnectAppMetadata m2, boolean checkId) {
        assertThat(m1)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt", "events")
                .isEqualTo(m2);
        if (checkId) {
            assertThat(m1.getId()).isEqualTo(m2.getId());
        }
        assertThat(m1.getEvents()).hasSize(m2.getEvents().size());
        for (int i = 0; i < m1.getEvents().size(); i++) {
            assertThat(m1.getEvents().get(i).getTimestamp()).isCloseTo(m2.getEvents().get(i).getTimestamp(), within(1, ChronoUnit.SECONDS));
            assertThat(m1.getEvents().get(i).getEventType()).isEqualTo(m2.getEvents().get(i).getEventType());
        }
    }

    @Test
    public void testSimpleInsert() throws SQLException {
        var m1 = createMetadataWithDefaultValues();
        var id = service.insert("", m1);

        var retrieved1 = service.get("", id).get();
        assertThat(retrieved1)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt", "events")
                .isEqualTo(m1);
        assertCompare(retrieved1, m1, false);
        assertThat(retrieved1.getCreatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(retrieved1.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        DefaultObjectMapper.prettyPrint(retrieved1);
    }

    @Test
    public void testDuplicateInsert() throws SQLException {
        var m1 = createMetadataWithDefaultValues();
        var id1 = service.insert("", m1);

        assertThatThrownBy(() -> service.insert("", m1))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("duplicate key value violates unique constraint");
    }

    @Test
    public void testUpsert() throws SQLException {
        var m1 = createMetadataWithDefaultValues();
        var id1 = service.insert("", m1);
        var retrieved1 = service.get("", id1).get();

        var m2 = m1.toBuilder()
                .atlassianBaseUrl("atlassianBaseUrl2")
                .events(List.of(
                        AtlassianConnectEvent.builder()
                                .eventType("install")
                                .timestamp(Instant.now())
                                .build(),
                        AtlassianConnectEvent.builder()
                                .eventType("enable")
                                .timestamp(Instant.now())
                                .build()))
                .build();
        var id2 = service.upsert(m2);
        var retrieved2 = service.get("", id1).get();
        assertThat(id2).isEqualTo(id1);
        assertCompare(retrieved2, m2, false);
        assertThat(retrieved2.getCreatedAt()).isEqualTo(retrieved1.getCreatedAt());
        assertThat(retrieved2.getUpdatedAt()).isAfter(retrieved1.getUpdatedAt());
    }

    @Test
    public void testUpsertDuplicate() throws SQLException {
        var m1 = createMetadataWithDefaultValues();
        var id1 = service.insert("", m1);
        var id2 = service.upsert(m1);
        assertThat(id2).isEqualTo(id1);
    }

    @Test
    public void testFailedUpdate() throws SQLException {
        var m1 = createMetadataWithDefaultValues().toBuilder()
                .id(UUID.randomUUID().toString())
                .build();
        var updated = service.update("", m1);
        assertThat(updated).isFalse();
    }

    @Test
    public void testSuccessfulUpdate() throws SQLException {
        var m1 = createMetadataWithDefaultValues().toBuilder()
                .id(UUID.randomUUID().toString())
                .build();
        var id1 = service.insert("", m1);
        var m2 = m1.toBuilder()
                .id(id1)
                .atlassianBaseUrl("atlassianBaseUrl2")
                .events(List.of(
                        AtlassianConnectEvent.builder()
                                .eventType("install")
                                .timestamp(Instant.now())
                                .build(),
                        AtlassianConnectEvent.builder()
                                .eventType("enable")
                                .timestamp(Instant.now())
                                .build()))
                .build();
        var updated = service.update("", m2);
        assertThat(updated).isTrue();
        assertCompare(service.get("", id1).get(), m2, true);
    }

    @Test
    public void testFilter() throws SQLException {
        var metadatas = List.of(
                createMetadataWithDefaultValues(),
                createMetadataWithDefaultValues().toBuilder()
                        .atlassianClientKey("atlassianClientKey2")
                        .build(),
                createMetadataWithDefaultValues().toBuilder()
                        .atlassianClientKey("atlassianClientKey3")
                        .build(),
                createMetadataWithDefaultValues().toBuilder()
                        .atlassianClientKey("atlassianClientKey4")
                        .enabled(false)
                        .build()
        );
        var ids = metadatas.stream().map(m -> {
            try {
                return service.insert("", m);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        var returned = service.filter(0, 10, AtlassianConnectAppMetadataFilter.builder()
                .atlassianClientKeys(List.of("atlassianClientKey2", "atlassianClientKey3"))
                .build()).getRecords();
        assertThat(returned).hasSize(2);
        assertCompare(returned.get(0), metadatas.get(2), false);
        assertCompare(returned.get(1), metadatas.get(1), false);

        returned = service.filter(0, 10, AtlassianConnectAppMetadataFilter.builder()
                .atlassianClientKeys(List.of("atlassianClientKey", "atlassianClientKey2", "atlassianClientKey3"))
                .build()).getRecords();
        assertThat(returned).map(AtlassianConnectAppMetadata::getId).containsExactlyInAnyOrderElementsOf(ids.subList(0, 3));

        returned = service.filter(0, 10, AtlassianConnectAppMetadataFilter.builder()
                .atlassianClientKeys(List.of("atlassianClientKey", "atlassianClientKey2", "atlassianClientKey3", "atlassianClientKey4"))
                .enabled(true)
                .build()).getRecords();
        assertThat(returned).map(AtlassianConnectAppMetadata::getId).containsExactlyInAnyOrderElementsOf(ids.subList(0, 3));

        returned = service.filter(0, 10, AtlassianConnectAppMetadataFilter.builder()
                .atlassianClientKeys(List.of("atlassianClientKey", "atlassianClientKey2", "atlassianClientKey3", "atlassianClientKey4"))
                .enabled(false)
                .build()).getRecords();
        assertThat(returned).map(AtlassianConnectAppMetadata::getId).containsExactlyInAnyOrderElementsOf(ids.subList(3, 4));
    }

    @Test
    public void testOtpFilter() {
        var metadatas = List.of(
                createMetadataWithDefaultValues(),
                createMetadataWithDefaultValues().toBuilder()
                        .atlassianClientKey("1")
                        .otp("")
                        .build(),
                createMetadataWithDefaultValues().toBuilder()
                        .atlassianClientKey("2")
                        .otp("abcd")
                        .enabled(false)
                        .build(),
                createMetadataWithDefaultValues().toBuilder()
                        .atlassianClientKey("3")
                        .otp("zxcv")
                        .build()
        );

        var ids = metadatas.stream().map(m -> {
            try {
                return service.insert("", m);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        var returned = service.stream(AtlassianConnectAppMetadataFilter.builder()
                .otp("abcd")
                .build()).collect(Collectors.toList());
        assertThat(returned).map(AtlassianConnectAppMetadata::getAtlassianClientKey).containsExactlyInAnyOrder("2");

        returned = service.stream(AtlassianConnectAppMetadataFilter.builder()
                .otp("abcd")
                .enabled(true)
                .build()).collect(Collectors.toList());
        assertThat(returned).isEmpty();


        returned = service.stream(AtlassianConnectAppMetadataFilter.builder()
                .otp(null)
                .enabled(true)
                .build()).collect(Collectors.toList());
        assertThat(returned).map(AtlassianConnectAppMetadata::getAtlassianClientKey).containsExactlyInAnyOrder("atlassianClientKey", "1", "3");
    }
}