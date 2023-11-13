package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SlackUsersDatabaseServiceTest {
    private static final String company = "test";
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private SlackUsersDatabaseService slackUsersDatabaseService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        slackUsersDatabaseService = new SlackUsersDatabaseService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        slackUsersDatabaseService.ensureTableExistence(company);
    }

    private void verifyRecord(SlackUser e, SlackUser a) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getTeamId(), e.getTeamId());
        Assert.assertEquals(a.getUserId(), e.getUserId());
        Assert.assertEquals(a.getRealNameNormalized(), e.getRealNameNormalized());
        Assert.assertEquals(a.getUsername(), e.getUsername());
        Assert.assertEquals(a.getUserId(), e.getUserId());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    private void testLookup(Map<Integer, SlackUser> m) throws SQLException {
        for(Integer i : m.keySet()) {
            SlackUser expected = m.get(i);
            List<SlackUser> slackUsers = slackUsersDatabaseService.lookup(company, expected.getTeamId(), expected.getUserId());
            Assert.assertEquals(1, slackUsers.size());
            verifyRecord(expected, slackUsers.get(0));
        }
    }

    private void testSingleIteration(Integer i, int iteration, Map<Integer, SlackUser> m) throws SQLException {
        SlackUser slackUser = SlackUser.builder()
                .teamId("team-"  + i)
                .userId("user-" + i)
                .realNameNormalized("real-name-normalized-" + i + "-" + iteration)
                .username("username-" + i + "-" + iteration)
                .email("email-" + i + "-" + iteration)
                .build();
        String idString = slackUsersDatabaseService.upsert(company, slackUser);
        UUID id = UUID.fromString(idString);
        slackUser = slackUser.toBuilder().id(id).build();
        if(m.containsKey(i)) {
            Assert.assertEquals(m.get(i).getId(), id);
        }
        m.put(i, slackUser);
        testLookup(m);
    }

    private void testMultipleIterations(int i, int iterationsCount, Map<Integer, SlackUser> m) throws SQLException {
        for(int it =0; it< iterationsCount; it++) {
            testSingleIteration(i, it, m);
        }
    }

    @Test
    public void test() throws SQLException {
        Map<Integer, SlackUser> m = new HashMap<>();
        int n =5;
        int iterationsCount = 5;
        for(int i=0; i< n; i++) {
            testMultipleIterations(i, iterationsCount, m);
        }
    }
}