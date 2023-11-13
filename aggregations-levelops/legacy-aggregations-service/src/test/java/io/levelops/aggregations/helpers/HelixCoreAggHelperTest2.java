package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class HelixCoreAggHelperTest2 {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final List<IntegrationConfig.RepoConfigEntry> REPO_CONFIG_ENTRY_LIST = List.of(
            IntegrationConfig.RepoConfigEntry.builder().repoId("depot").pathPrefix("//depot/").build()
    );
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static final String CUSTOMER = "test";
    private static String INTEGRATION_ID = "1";

    private ScmAggService scmAggService;

    @Before
    public void setup() throws SQLException {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        IntegrationService integrationService = new IntegrationService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(CUSTOMER);
        integrationService.ensureTableExistence(CUSTOMER);
        INTEGRATION_ID = integrationService.insert(CUSTOMER, Integration.builder()
                .application("helix")
                .name("helix test")
                .status("enabled")
                .build());
        integrationService.insertConfig(CUSTOMER, IntegrationConfig.builder().integrationId(INTEGRATION_ID)
                .repoConfig(REPO_CONFIG_ENTRY_LIST)
                .build());
        userIdentityService.ensureTableExistence(CUSTOMER);
        scmAggService.ensureTableExistence(CUSTOMER);
    }

    // LEV-4751: Need to update the change volume stats column...
    //ToDo: Need to remove after some time...
    @Test
    public void testChangeVolumeStatsUpdate() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_core_changelists2.json");
        HelixCoreChangeList changeList = MAPPER.readValue(content, HelixCoreChangeList.class);

        List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist = IntegrationWhitelistEntry.fromCommaSeparatedString("abcd:1234");
        HelixCoreAggHelper helixCoreAggHelper = new HelixCoreAggHelper(null, scmAggService, null, scmCommitsInsertV2integrationIdWhitelist);
        boolean useScmCommitInsertV2 = ScmAggUtils.useScmCommitsInsertV2(scmCommitsInsertV2integrationIdWhitelist, CUSTOMER, INTEGRATION_ID);
        helixCoreAggHelper.processHelixChangeList(CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_LIST, changeList, Map.of(), useScmCommitInsertV2);
        List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, INTEGRATION_ID, REPO_CONFIG_ENTRY_LIST);
        Set<String> repoIds = dbScmFiles.stream().map(DbScmFile::getRepoId).collect(Collectors.toSet());
        Optional<DbScmCommit> optDbScmCommit = scmAggService.getCommit(CUSTOMER, String.valueOf(changeList.getId()), new ArrayList<>(repoIds), INTEGRATION_ID);
        Assertions.assertThat(optDbScmCommit.isPresent()).isEqualTo(true);
        DbScmCommit dbScmCommit = optDbScmCommit.get();
        Assertions.assertThat(dbScmCommit.getAdditions()).isEqualTo(0);
        Assertions.assertThat(dbScmCommit.getDeletions()).isEqualTo(0);
        Assertions.assertThat(dbScmCommit.getChanges()).isEqualTo(8);
        for (DbScmFile dbScmFile : dbScmFiles) {
            Optional<DbScmFile> optDbScmFile = scmAggService.getFile(CUSTOMER, dbScmFile.getFilename(),
                    dbScmFile.getRepoId(), dbScmFile.getProject(), INTEGRATION_ID);
            if (optDbScmFile.isPresent()) {
                Optional<DbScmFileCommit> optDbScmFileCommit = scmAggService.getFileCommit(CUSTOMER,
                        dbScmCommit.getCommitSha(), optDbScmFile.get().getId());
                if (optDbScmFileCommit.isPresent()) {
                    DbScmFileCommit dbScmFileCommit = optDbScmFileCommit.get();
                    Assertions.assertThat(dbScmFileCommit.getAddition()).isEqualTo(0);
                    Assertions.assertThat(dbScmFileCommit.getDeletion()).isEqualTo(0);
                    Assertions.assertThat(dbScmFileCommit.getChange()).isEqualTo(4);
                }
            }
        }

//        helixCoreAggHelper.processHelixChangeList(CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_LIST, changeList, Map.of());
//        optDbScmCommit = scmAggService.getCommit(CUSTOMER, String.valueOf(changeList.getId()), new ArrayList<>(repoIds), INTEGRATION_ID);
//        Assertions.assertThat(optDbScmCommit.isPresent()).isEqualTo(true);
//        dbScmCommit = optDbScmCommit.get();
//        Assertions.assertThat(dbScmCommit.getAdditions()).isEqualTo(8);
//        Assertions.assertThat(dbScmCommit.getDeletions()).isEqualTo(0);
//        Assertions.assertThat(dbScmCommit.getChanges()).isEqualTo(0);
//        for (DbScmFile dbScmFile : dbScmFiles) {
//            Optional<DbScmFile> optDbScmFile = scmAggService.getFile(CUSTOMER, dbScmFile.getFilename(),
//                    dbScmFile.getRepoId(), dbScmFile.getProject(), INTEGRATION_ID);
//            if (optDbScmFile.isPresent()) {
//                Optional<DbScmFileCommit> optDbScmFileCommit = scmAggService.getFileCommit(CUSTOMER,
//                        dbScmCommit.getCommitSha(), optDbScmFile.get().getId());
//                if (optDbScmFileCommit.isPresent()) {
//                    DbScmFileCommit dbScmFileCommit = optDbScmFileCommit.get();
//                    Assertions.assertThat(dbScmFileCommit.getAddition()).isEqualTo(4);
//                    Assertions.assertThat(dbScmFileCommit.getDeletion()).isEqualTo(0);
//                    Assertions.assertThat(dbScmFileCommit.getChange()).isEqualTo(0);
//                }
//            }
//        }

    }
    //endregion
}
