package io.levelops.commons.databases.services.scm;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmCommitPRMapping;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class ScmCommitPullRequestMappingDBServiceTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;

    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static ScmAggService scmAggService;
    private static ScmCommitPullRequestMappingDBService scmCommitPullRequestMappingDBService;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmCommitPullRequestMappingDBService = new ScmCommitPullRequestMappingDBService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        scmCommitPullRequestMappingDBService.ensureTableExistence(company);



    }

    private void verifyRecord(DbScmCommitPRMapping a, DbScmCommitPRMapping e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getScmCommitId(), e.getScmCommitId());
        Assert.assertEquals(a.getScmPullrequestId(), e.getScmPullrequestId());
        Assert.assertNotNull(a.getCreatedAt());
    }
    private void verifyRecords(List<DbScmCommitPRMapping> a, List<DbScmCommitPRMapping> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, DbScmCommitPRMapping> actualMap = a.stream().collect(Collectors.toMap(DbScmCommitPRMapping::getId, x -> x));
        Map<UUID, DbScmCommitPRMapping> expectedMap = e.stream().collect(Collectors.toMap(DbScmCommitPRMapping::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    @Test
    public void testCRUD() throws SQLException {
        Assert.assertEquals(0, scmCommitPullRequestMappingDBService.list(company, 0, 100).getRecords().size());

        Integration integration = IntegrationUtils.createIntegrations(integrationService, company, 1).get(0);
        DbScmUser user = UserIdentityUtils.createUser(userIdentityService, company, Integer.parseInt(integration.getId()), 0);
        List<DbScmCommit> scmCommits = ScmAggUtils.createScmCommits(scmAggService, company, user, 2);
        List<DbScmPullRequest> pullRequests = ScmAggUtils.createPullRequests(scmAggService, company, user, 2);



        List<DbScmCommitPRMapping> expected = List.of(
                DbScmCommitPRMapping.builder().scmCommitId(UUID.fromString(scmCommits.get(0).getId())).scmPullrequestId(UUID.fromString(pullRequests.get(0).getId())).build(),
                DbScmCommitPRMapping.builder().scmCommitId(UUID.fromString(scmCommits.get(0).getId())).scmPullrequestId(UUID.fromString(pullRequests.get(1).getId())).build(),
                DbScmCommitPRMapping.builder().scmCommitId(UUID.fromString(scmCommits.get(1).getId())).scmPullrequestId(UUID.fromString(pullRequests.get(0).getId())).build(),
                DbScmCommitPRMapping.builder().scmCommitId(UUID.fromString(scmCommits.get(1).getId())).scmPullrequestId(UUID.fromString(pullRequests.get(1).getId())).build()
        );


        scmCommitPullRequestMappingDBService.batchInsert(company, expected);
        DbListResponse<DbScmCommitPRMapping> actual = scmCommitPullRequestMappingDBService.list(company, 0, 100);
        Assert.assertEquals(4, actual.getRecords().size());
        //verifyRecords(expected, actual.getRecords());

        scmCommitPullRequestMappingDBService.batchInsert(company, expected);
        actual = scmCommitPullRequestMappingDBService.list(company, 0, 100);
        Assert.assertEquals(4, actual.getRecords().size());
        //verifyRecords(expected, actual.getRecords());
    }
}