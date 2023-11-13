package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HelixChangeListTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String helixIntegrationId;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        helixIntegrationId = integrationService.insert(company, Integration.builder()
                .application("helix")
                .name("helix test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        List<IntegrationConfig.RepoConfigEntry> configEntries = List.of(IntegrationConfig.RepoConfigEntry.builder()
                .repoId("Depot").pathPrefix("//Depot").build());
        String input = ResourceUtils.getResourceAsString("json/databases/helix_changelist_change_volume.json");
        PaginatedResponse<HelixCoreChangeList> helixChangelist = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, HelixCoreChangeList.class));
        helixChangelist.getResponse().getRecords().forEach(changeList -> {
            List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, "1", configEntries);
            Set<String> repoIds = dbScmFiles.stream().map(DbScmFile::getRepoId).collect(Collectors.toSet());
            DbScmCommit helixCoreCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, "1");
            if (CollectionUtils.isNotEmpty(repoIds) && scmAggService.getCommit(company, helixCoreCommit.getCommitSha(),
                    helixCoreCommit.getRepoIds(), "1").isEmpty() && dbScmFiles.size() > 0) {
                try {
                    scmAggService.insertCommit(company, helixCoreCommit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                dbScmFiles.forEach(dbScmFile -> scmAggService.insertFile(company, dbScmFile));
            }
        });
    }

    @Test
    public void testAdditionsAndDeletions() {
        Assertions.assertThat(scmAggService.getCommit(company, "20", "Depot", helixIntegrationId)
                .orElse(null).getAdditions()).isEqualTo(0);
        Assertions.assertThat(scmAggService.getCommit(company, "20", "Depot", helixIntegrationId)
                .orElse(null).getDeletions()).isEqualTo(1);
        Assertions.assertThat(scmAggService.getCommit(company, "6", "Depot", helixIntegrationId)
                .orElse(null).getAdditions()).isEqualTo(1);
        Assertions.assertThat(scmAggService.getCommit(company, "6", "Depot", helixIntegrationId)
                .orElse(null).getDeletions()).isEqualTo(0);

    }
}
