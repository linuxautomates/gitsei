package io.levelops.contributor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.contributor.model.SEIContributor;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SEIContributorServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

   @ClassRule
   public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static OrgUsersDatabaseService orgUsersService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static SEIContributorService seiContributorService;

    private static Integration integration1, integration2, integration3;
    private static Integer integrationId1, integrationId2, integrationId3;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        orgUsersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        OrgUsersLockService orgUsersLockService = mock(OrgUsersLockService.class);
        when(orgUsersLockService.lock(anyString(), anyInt())).thenReturn(true);
        when(orgUsersLockService.unlock(anyString())).thenReturn(true);
        orgUsersService.ensureTableExistence(company);
        UserService userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);

        var firstVersion = orgVersionsService.insert(company, OrgVersion.OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);

        seiContributorService = new SEIContributorService(dataSource);

        integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("github")
                .status("active")
                .build();
        integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        integration2 = Integration.builder()
                .description("description1")
                .name("integ2")
                .url("url")
                .application("github")
                .status("active")
                .build();
        integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));

        integration3 = Integration.builder()
                .description("description1")
                .name("integ3")
                .url("url")
                .application("bitbucket")
                .status("active")
                .build();
        integrationId3 = Integer.valueOf(integrationService.insert(company, integration3));
    }


    @Test
    public void testList() throws SQLException {
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId1").username("cloudId1").integrationType(integration1.getApplication()).integrationId(integrationId1).build(),
                        DBOrgUser.LoginId.builder().cloudId("cloudId2").username("cloudId2").integrationType(integration2.getApplication()).integrationId(integrationId2).build()))
                .versions(Set.of(1))
                .build();
        Integer.valueOf(orgUsersService.insert(company, orgUser1));

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId3").username("cloudId3").integrationType(integration1.getApplication()).integrationId(integrationId1).build(),
                        DBOrgUser.LoginId.builder().cloudId("cloudId4").username("cloudId4").integrationType(integration3.getApplication()).integrationId(integrationId3).build()))
                .versions(Set.of(1))
                .build();
        Integer.valueOf(orgUsersService.insert(company, orgUser2));

        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId5").username("cloudId5").integrationType(integration3.getApplication()).integrationId(integrationId3).build()))
                .versions(Set.of(1))
                .build();
        Integer.valueOf(orgUsersService.insert(company, orgUser3));

        SEIContributor seiContributor = seiContributorService.list(company);
        Assertions.assertThat(seiContributor).isNotNull();
        Assertions.assertThat(seiContributor.getOrgUserCount()).isEqualTo(3);
        Assertions.assertThat(seiContributor.getIntegrationUserCount()).isEqualTo(5);

    }
}
