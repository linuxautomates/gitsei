package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class UserIdentityMaskingServiceTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static UserIdentityService userIdentityService;
    private static TenantConfigService tenantConfigService;
    private static OrgUsersDatabaseService orgUsersDatabaseService;
    private static UserIdentityMaskingService userIdentityMaskingService;
    private static NamedParameterJdbcTemplate template;
    private static ObjectMapper mapper;
    private static OrgVersionsDatabaseService versionsService;
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);
        tenantConfigService = new TenantConfigService(dataSource);
        tenantConfigService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        mapper = new ObjectMapper();
        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, mapper, versionsService, userIdentityService);
        orgUsersDatabaseService.ensureTableExistence(company);
        userIdentityMaskingService = new UserIdentityMaskingService(dataSource, tenantConfigService, orgUsersDatabaseService, userIdentityService);
        tenantConfigService.insert(company, TenantConfig.builder().name("HIDE_EXTERNAL_USER_INFO").value("true").build());
        tenantConfigService.insert(company, TenantConfig.builder().name("SUPPORTED_INTERNAL_DOMAIN").value("encora.com").build());
    }

    @Test
    public void testMasking() throws SQLException {
        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
                .description("description1")
                .name("integ2")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));

        var integration3 = Integration.builder()
                .description("description1")
                .name("integ3")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId3 = Integer.valueOf(integrationService.insert(company, integration3));

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("Satish Kumar Singh")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = Integer.valueOf(orgUsersDatabaseService.insert(company, orgUser1));

        DBOrgUser dbUser1 = orgUsersDatabaseService.getByUser(company, orgUser1.getFullName()).get();
        Assertions.assertThat(dbUser1).isEqualTo(orgUser1.toBuilder()
                .id(dbUser1.getId())
                .refId(userId1)
                .createdAt(dbUser1.getCreatedAt())
                .updatedAt(dbUser1.getUpdatedAt())
                .build());
        String maskUser =null;
        if(userIdentityMaskingService.isMasking(company, "1", "abc", "Satish Kumar Singh")){
            maskUser=userIdentityMaskingService.maskedUser(company);
        }else{
            maskUser="Satish Kumar Singh";
        }
      //  String maskUser = userIdentityMaskingService.getDisplayName(company, "1", "abc", "Satish Kumar Singh");
        Assertions.assertThat("Satish Kumar Singh").isEqualTo(maskUser);
        var orgUser2 = DBOrgUser.builder()
                .email("satish@abc.com")
                .fullName("Satish Kumar")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId2 = Integer.valueOf(orgUsersDatabaseService.insert(company, orgUser2));
        DBOrgUser dbUser2 = orgUsersDatabaseService.getByUser(company, orgUser2.getFullName()).get();
        String maskUser1 = null;
        if(userIdentityMaskingService.isMasking(company, "1", "abc", "satish")) {
            maskUser1=userIdentityMaskingService.maskedUser(company);
        }else{
            maskUser1="satish";
        }
        Assertions.assertThat("External User1").isEqualTo(maskUser1);
        var orgUser3 = DBOrgUser.builder()
                .email("satish@test.com")
                .fullName("Satish")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId3 = Integer.valueOf(orgUsersDatabaseService.insert(company, orgUser3));
        DBOrgUser dbUser3 = orgUsersDatabaseService.getByUser(company, orgUser3.getFullName()).get();
        String maskUser2 = null;
        if(userIdentityMaskingService.isMasking(company, "1", "abc", "Satish Kumar")){
            maskUser2=userIdentityMaskingService.maskedUser(company);
        }else{
            maskUser2="Satish Kumar";
        }
        Assertions.assertThat("Satish Kumar").isEqualTo(maskUser2);
        var orgUser4 = DBOrgUser.builder()
                .email("satish@encora.com")
                .fullName("Satish Singh")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId4 = Integer.valueOf(orgUsersDatabaseService.insert(company, orgUser4));
        DBOrgUser dbUser4 = orgUsersDatabaseService.getByUser(company, orgUser2.getFullName()).get();
        String maskUser3 =null;
        if(userIdentityMaskingService.isMasking(company, "1", "abc", "satish@encora.com")){
            maskUser3=userIdentityMaskingService.maskedUser(company);
        }else{
            maskUser3="satish@encora.com";
        }
        Assertions.assertThat("satish@encora.com").isEqualTo(maskUser3);

    }

}
