package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class OrgVersionsDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static OrgVersionsDatabaseService orgVersionsService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
            "CREATE SCHEMA IF NOT EXISTS " + company,
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
        .forEach(template.getJdbcTemplate()::execute);

        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
    }

    @Test
    public void testEmpty() throws SQLException {
        Assertions.assertThat(orgVersionsService.getLatest(company, OrgAssetType.UNIT)).isNotPresent();
        Assertions.assertThat(orgVersionsService.getLatest(company, OrgAssetType.USER)).isNotPresent();
    }

    @Test
    public void testVe() throws SQLException {
        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);

        var versions = orgVersionsService.filter(company, OrgAssetType.USER, 0, 10);
        Assertions.assertThat(versions.getTotalCount()).isEqualTo(1);
    }

}
