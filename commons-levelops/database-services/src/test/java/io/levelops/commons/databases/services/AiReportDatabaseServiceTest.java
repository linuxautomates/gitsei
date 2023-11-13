package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.AiReport;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AiReportDatabaseServiceTest {

    private static final String COMPANY = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    AiReportDatabaseService aiReportDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();

        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);

        aiReportDatabaseService = new AiReportDatabaseService(dataSource, DefaultObjectMapper.get());
        aiReportDatabaseService.ensureTableExistence(COMPANY);
    }

    @Test
    public void test() throws SQLException, InterruptedException {
        // -- insert both data and error
        AiReport r1 = AiReport.builder()
                .type("t")
                .key("1")
                .data(Map.of("data", "1"))
                .error(Map.of("error", "1"))
                .build();
        String id1 = aiReportDatabaseService.insert(COMPANY, r1);

        AiReport get1 = aiReportDatabaseService.get(COMPANY, id1).orElse(null);
        assertThat(get1).isNotNull();
        assertThat(get1).usingRecursiveComparison().ignoringFields("id", "dataUpdatedAt", "errorUpdatedAt").isEqualTo(r1);
        assertThat(get1.getId()).isEqualTo(UUID.fromString(id1));
        assertThat(get1.getDataUpdatedAt()).isNotNull();
        assertThat(get1.getDataUpdatedAt()).isEqualTo(get1.getErrorUpdatedAt());

        AiReport get1ByTypeKey = aiReportDatabaseService.get(COMPANY, r1.getTypeKeyIdentifier()).orElse(null);
        assertThat(get1ByTypeKey).isEqualTo(get1);

        // -- insert only data
        AiReport r2 = AiReport.builder()
                .type("t")
                .key("2")
                .data(Map.of("data", "2"))
                .build();
        String id2 = aiReportDatabaseService.insert(COMPANY, r2);

        AiReport get2 = aiReportDatabaseService.get(COMPANY, id2).orElse(null);
        assertThat(get2).isNotNull();
        assertThat(get2).usingRecursiveComparison().ignoringFields("id", "error", "dataUpdatedAt", "errorUpdatedAt").isEqualTo(r2);
        assertThat(get2.getId()).isEqualTo(UUID.fromString(id2));
        assertThat(get2.getError()).isNotNull();
        assertThat(get2.getError()).isEmpty();
        assertThat(get2.getDataUpdatedAt()).isNotNull();
        assertThat(get2.getDataUpdatedAt()).isEqualTo(get2.getErrorUpdatedAt());

        // -- insert only error
        AiReport r3 = AiReport.builder()
                .type("t")
                .key("3")
                .error(Map.of("error", "3"))
                .build();
        String id3 = aiReportDatabaseService.insert(COMPANY, r3);

        AiReport get3 = aiReportDatabaseService.get(COMPANY, id3).orElse(null);
        assertThat(get3).isNotNull();
        assertThat(get3).usingRecursiveComparison().ignoringFields("id", "data", "dataUpdatedAt", "errorUpdatedAt").isEqualTo(r3);
        assertThat(get3.getId()).isEqualTo(UUID.fromString(id3));
        assertThat(get3.getData()).isNotNull();
        assertThat(get3.getData()).isEmpty();
        assertThat(get3.getDataUpdatedAt()).isNotNull();
        assertThat(get3.getDataUpdatedAt()).isEqualTo(get3.getErrorUpdatedAt());

        // -- upsert data
        AiReport r1update = AiReport.builder()
                .type("t")
                .key("1")
                .data(Map.of("data", "updated"))
                .build();
        Thread.sleep(2);
        String id1Update = aiReportDatabaseService.insert(COMPANY, r1update);
        assertThat(id1Update).isEqualTo(id1);

        AiReport get1Update = aiReportDatabaseService.get(COMPANY, id1Update).orElse(null);
        assertThat(get1Update).isNotNull();
        assertThat(get1Update.getId()).isEqualTo(UUID.fromString(id1));
        assertThat(get1Update.getData()).isEqualTo(r1update.getData());
        assertThat(get1Update.getError()).isEqualTo(r1.getError());
        assertThat(get1Update.getDataUpdatedAt()).isAfter(get1.getDataUpdatedAt());
        assertThat(get1Update.getErrorUpdatedAt()).isEqualTo(get1.getErrorUpdatedAt());

        // -- update data
        AiReport r1update2 = AiReport.builder()
                .type("t")
                .key("1")
                .data(Map.of("data", "updated2"))
                .build();
        Thread.sleep(2);
        aiReportDatabaseService.update(COMPANY, r1update2);

        AiReport get1Update2 = aiReportDatabaseService.get(COMPANY, r1update2.getTypeKeyIdentifier()).orElse(null);
        assertThat(get1Update2).isNotNull();
        assertThat(get1Update2.getId()).isEqualTo(UUID.fromString(id1));
        assertThat(get1Update2.getData()).isEqualTo(r1update2.getData());
        assertThat(get1Update2.getError()).isEqualTo(r1.getError());
        assertThat(get1Update2.getDataUpdatedAt()).isAfter(get1Update.getDataUpdatedAt());
        assertThat(get1Update2.getErrorUpdatedAt()).isEqualTo(get1Update.getErrorUpdatedAt());

        // -- upsert error
        AiReport r2update = AiReport.builder()
                .type("t")
                .key("2")
                .error(Map.of("error", "updated"))
                .build();
        Thread.sleep(2);
        String id2Update = aiReportDatabaseService.insert(COMPANY, r2update);
        assertThat(id2Update).isEqualTo(id2);

        AiReport get2Update = aiReportDatabaseService.get(COMPANY, id2Update).orElse(null);
        assertThat(get2Update).isNotNull();
        assertThat(get2Update.getId()).isEqualTo(UUID.fromString(id2));
        assertThat(get2Update.getData()).isEqualTo(get2.getData());
        assertThat(get2Update.getError()).isEqualTo(r2update.getError());
        assertThat(get2Update.getDataUpdatedAt()).isEqualTo(get2.getDataUpdatedAt());
        assertThat(get2Update.getErrorUpdatedAt()).isAfter(get2.getErrorUpdatedAt());

        // -- update error
        AiReport r2update2 = AiReport.builder()
                .type("t")
                .key("2")
                .error(Map.of("error", "updated2"))
                .build();
        Thread.sleep(2);
        String id2Update2 = aiReportDatabaseService.insert(COMPANY, r2update2);
        assertThat(id2Update2).isEqualTo(id2);

        AiReport get2Update2 = aiReportDatabaseService.get(COMPANY, id2Update2).orElse(null);
        assertThat(get2Update2).isNotNull();
        assertThat(get2Update2.getId()).isEqualTo(UUID.fromString(id2));
        assertThat(get2Update2.getData()).isEqualTo(get2.getData());
        assertThat(get2Update2.getError()).isEqualTo(r2update2.getError());
        assertThat(get2Update2.getDataUpdatedAt()).isEqualTo(get2Update.getDataUpdatedAt());
        assertThat(get2Update2.getErrorUpdatedAt()).isAfter(get2Update.getErrorUpdatedAt());

        // -- filter
        assertThat(aiReportDatabaseService.stream(COMPANY, AiReportDatabaseService.AiReportFilter.builder()
                .types(List.of("t"))
                .build()).map(AiReport::getId).map(UUID::toString)
        ).containsExactlyInAnyOrder(id1, id2, id3);
        assertThat(aiReportDatabaseService.stream(COMPANY, AiReportDatabaseService.AiReportFilter.builder()
                .types(List.of("dummy"))
                .build())
        ).isEmpty();
        assertThat(aiReportDatabaseService.stream(COMPANY, AiReportDatabaseService.AiReportFilter.builder()
                .types(List.of("t"))
                .keys(List.of("2"))
                .build()).map(AiReport::getId).map(UUID::toString)
        ).containsExactlyInAnyOrder(id2);
        assertThat(aiReportDatabaseService.stream(COMPANY, AiReportDatabaseService.AiReportFilter.builder()
                .typeKeyIdentifiers(List.of(r1.getTypeKeyIdentifier(), r3.getTypeKeyIdentifier()))
                .build()).map(AiReport::getId).map(UUID::toString)
        ).containsExactlyInAnyOrder(id1, id3);

        // -- pagination
        List<AiReport> page1 = aiReportDatabaseService.list(COMPANY, 0, 2).getRecords();
        List<AiReport> page2 = aiReportDatabaseService.list(COMPANY, 1, 2).getRecords();
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1);
        assertThat(Stream.of(page1, page2).flatMap(Collection::stream).map(AiReport::getId).map(UUID::toString)).containsExactlyInAnyOrder(id1, id2, id3);

        // -- delete
        aiReportDatabaseService.delete(COMPANY, id1);
        assertThat(aiReportDatabaseService.get(COMPANY, id1)).isEmpty();
    }

}