package io.levelops.commons.databases.services.organization;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrgUsersDatabaseServiceTestUtils {
    public static DBOrgUser createDBOrgUser(OrgUsersDatabaseService orgUsersDatabaseService, String company, int i, Integration integration) throws SQLException {
        DBOrgUser orgUser = DBOrgUser.builder()
                .email("email" + i)
                .fullName("fullName" + i)
                .customFields(Map.of("test_name", "test" + i))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration.getApplication()).integrationId(Integer.parseInt(integration.getId())).build()))
                .versions(Set.of(1))
                .build();
        OrgUserId userId = orgUsersDatabaseService.upsert(company, orgUser);
        return orgUser.toBuilder().id(userId.getId()).refId(userId.getRefId()).build();
    }

    public static List<DBOrgUser> createDBOrgUsers(OrgUsersDatabaseService orgUsersDatabaseService, String company, int n, Integration integration) throws SQLException {
        List<DBOrgUser> result = new ArrayList<>();
        for (int i =0; i<n; i++) {
            DBOrgUser dbOrgUser = createDBOrgUser(orgUsersDatabaseService, company, i, integration);
            result.add(dbOrgUser);
        }
        return result;
    }
}
