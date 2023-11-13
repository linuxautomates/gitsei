package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.okta.DbOktaAssociation;
import io.levelops.commons.databases.models.database.okta.DbOktaGroup;
import io.levelops.commons.databases.models.database.okta.DbOktaUser;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class OktaAggServiceTest {

    private static final String COMPANY = "test";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private OktaAggService oktaAggService;

    @Before
    public void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        oktaAggService = new OktaAggService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("okta")
                .name("okta_test")
                .status("enabled")
                .build());
        Date currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        oktaAggService.ensureTableExistence(COMPANY);
        final String usersInput = ResourceUtils.getResourceAsString("json/databases/okta_users.json");
        PaginatedResponse<OktaUser> paginatedUsers = OBJECT_MAPPER.readValue(usersInput,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, OktaUser.class));
        List<OktaUser> users = paginatedUsers.getResponse().getRecords();
        List<DbOktaUser> dbOktaUsers;
        String integrationId = "1";
        dbOktaUsers = users.stream()
                .map(user -> DbOktaUser.fromOktaUSer(user, integrationId, currentTime))
                .collect(Collectors.toList());
        dbOktaUsers.forEach(user -> oktaAggService.insert(COMPANY, user));
        final String groupsInput = ResourceUtils.getResourceAsString("json/databases/okta_groups.json");
        PaginatedResponse<OktaGroup> paginatedGroups = OBJECT_MAPPER.readValue(groupsInput,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, OktaGroup.class));
        List<OktaGroup> groups = paginatedGroups.getResponse().getRecords();
        List<DbOktaGroup> dbGroups = new ArrayList<>();
        for (OktaGroup group : groups) {
            dbGroups.add(DbOktaGroup.fromOktaGroup(group, integrationId, currentTime));
        }
        dbGroups.forEach(group -> oktaAggService.insert(COMPANY, group));
        users.forEach(user -> {
            if (user.getAssociatedMembers() != null) {
                user.getAssociatedMembers().forEach(associatedMembers -> {
                    associatedMembers.getAssociatedMembers().forEach(memberId -> {
                        oktaAggService.insert(COMPANY,
                                DbOktaAssociation.fromOktaAssociation(user.getId(), memberId, associatedMembers, integrationId, currentTime));
                    });
                });
            }
        });
    }

    @Test
    public void test() throws SQLException {
        assertThat(oktaAggService.list(COMPANY, 0, 100).getCount()).isEqualTo(7);
        assertThat(oktaAggService.listAssociations(COMPANY, 0, 100).getCount()).isEqualTo(2);
        assertThat(oktaAggService.lisGroups(COMPANY, 0, 100).getCount()).isEqualTo(6);
    }
}
