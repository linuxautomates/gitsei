package io.levelops.commons.databases.services.scm;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.UserIdentityService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserIdentityUtils {
    public static DbScmUser createUser(UserIdentityService userIdentityService, final String company, Integer integrationId, int i) throws SQLException {
        DbScmUser user = DbScmUser.builder()
                .integrationId(String.valueOf(integrationId))
                .displayName("display name " + i).originalDisplayName("original display name " + i).cloudId("cloud id " + i)
                .build();

        String id = userIdentityService.insert(company,user);
        return user.toBuilder().id(id).build();
    }

    public static List<DbScmUser> createUsers(UserIdentityService userIdentityService, final String company, Integer integrationId, int n) throws SQLException {
        List<DbScmUser> users = new ArrayList<>();
        for(int i=0; i<n; i++){
            DbScmUser user = createUser(userIdentityService, company, integrationId, i);
            users.add(user);
        }
        return users;
    }
}
