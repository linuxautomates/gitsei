package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.velocity.OrgProfile;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class OrgProfileDatabaseServiceTest {
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static NamedParameterJdbcTemplate template;
    private static OrgProfileDatabaseService orgProfileDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);

        orgProfileDatabaseService = new OrgProfileDatabaseService(dataSource, mapper);
        orgProfileDatabaseService.ensureTableExistence(company);
    }

    @Test
    public void test_getByProfileId_onSuccess() throws SQLException {
        UUID profileId = UUID.randomUUID();
        OrgProfile workflowProfile = OrgProfile.builder()
                .ouRefId(1)
                .profileId(profileId)
                .profileType(OrgProfile.ProfileType.WORKFLOW)
                .build();
        int noOfRowsAffected = orgProfileDatabaseService.insert(company, List.of(workflowProfile));
        Assert.assertEquals(1, noOfRowsAffected);
        Optional<List<String>> ouRefIds = orgProfileDatabaseService.getByProfileId(
                company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW
        );
        Assert.assertTrue(ouRefIds.isPresent());
        Assert.assertEquals(ouRefIds.get().get(0), "1");
    }

    @Test
    public void test_deleteByProfileId_onSuccess() throws SQLException {
        UUID profileId = UUID.randomUUID();
        OrgProfile workflowProfile = OrgProfile.builder()
                .ouRefId(2)
                .profileId(profileId)
                .profileType(OrgProfile.ProfileType.WORKFLOW)
                .build();
        int noOfRowsAffected = orgProfileDatabaseService.insert(company, List.of(workflowProfile));
        Assert.assertEquals(1, noOfRowsAffected);
        boolean result = orgProfileDatabaseService.deleteByProfileId(company, profileId);
        Assert.assertTrue(result);
    }

    @Test
    public void testUpdateOUAssociation() throws SQLException {
        UUID profileId = UUID.randomUUID();
        OrgProfile workflowProfile = OrgProfile.builder()
                .ouRefId(1)
                .profileId(profileId)
                .profileType(OrgProfile.ProfileType.WORKFLOW)
                .build();
        int noOfRowsAffected = orgProfileDatabaseService.insert(company, List.of(workflowProfile));
        orgProfileDatabaseService.updateProfileOUMappings(company,profileId,List.of("2","3"));
        Optional<List<String>> ouRefIds = orgProfileDatabaseService.getByProfileId(
                company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW
        );
        Assert.assertTrue(ouRefIds.isPresent());
        Assert.assertEquals(ouRefIds.get(), List.of("2","3"));
    }
}
