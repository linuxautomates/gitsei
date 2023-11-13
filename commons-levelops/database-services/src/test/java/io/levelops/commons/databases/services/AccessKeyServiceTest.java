package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.sql.SQLException;

import javax.sql.DataSource;

public class AccessKeyServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private AccessKeyService keyService;

    private DataSource dataSource;
    private String company = "test";

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        keyService = new AccessKeyService(dataSource);
        keyService.ensureTableExistence(company);
    }

    @Test
    public void testUpdate() throws SQLException {
        var id = keyService.insert(company, AccessKey.builder().name("test")
                .roleType(RoleType.ADMIN)
                .bcryptSecret("asd")
                .description("asd")
                .createdAt(123L)
                .build());
        Assert.assertEquals(RoleType.ADMIN, keyService.getForAuthOnly(company, id).get().getRoleType());
    }
}
