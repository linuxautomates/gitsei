package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class IntegrationAggServiceTest {

    private static final String COMPANY = "test";
    private final ObjectMapper mapper = DefaultObjectMapper.get();
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private IntegrationAggService integrationAggService;

    @Before
    public void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        integrationAggService = new IntegrationAggService(dataSource);
        integrationAggService.ensureTableExistence(COMPANY);
    }

    @Test
    public void testSimpleCrud() throws SQLException {
        var integrationId = "1";
        var integrationAgg = IntegrationAgg.builder()
                .integrationIds(List.of(integrationId))
                .successful(true)
                .gcsPath("testPath")
                .version("version1")
                .type(IntegrationAgg.AnalyticType.SNYK)
                .build();
        var integrationAggId = integrationAggService.insert(COMPANY, integrationAgg);

        var dbIntegrationAgg = integrationAggService.get(COMPANY, integrationAggId).get();
        assertThat(dbIntegrationAgg.getIntegrationIds()).isEqualTo(List.of(integrationId));
        assertThat(dbIntegrationAgg.getGcsPath()).isEqualTo("testPath");

        assertThat(integrationAggService.list(COMPANY, 0, 100).getTotalCount()).isEqualTo(1);

        var listResults = integrationAggService.listByFilter(COMPANY, List.of(IntegrationAgg.AnalyticType.SNYK), null, true, 0, 100);
        assertThat(listResults.getTotalCount()).isEqualTo(1);

        listResults = integrationAggService.listByFilter(COMPANY, List.of(IntegrationAgg.AnalyticType.JIRA), null, true, 0, 100);
        assertThat(listResults.getTotalCount()).isEqualTo(0);

        var updated = integrationAggService.update(COMPANY, integrationAgg.toBuilder()
                .version("updatedVersion").id(integrationAggId).build());
        assertThat(updated).isTrue();
        dbIntegrationAgg = integrationAggService.get(COMPANY, integrationAggId).get();
        assertThat(dbIntegrationAgg.getVersion()).isEqualTo("updatedVersion");

        var deleted = integrationAggService.delete(COMPANY, integrationAggId);
        assertThat(deleted).isTrue();
        assertThat(integrationAggService.list(COMPANY, 0, 100).getTotalCount()).isEqualTo(0);
    }
}
