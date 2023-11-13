package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserUtils {
    public static User createUser(UserService userService, String company, int i) throws SQLException {
        User user = User.builder()
                .userType(RoleType.LIMITED_USER)
                .bcryptPassword("asd")
                .email("asd" + i + "@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd" + i)
                .lastName("asd" + i)
                .build();
        String userId = userService.insert(company, user);
        return user.toBuilder().id(userId).build();
    }
    public static List<User> createUsers(UserService userService, String company, int n) throws SQLException {
        List<User> users = new ArrayList<>();
        for(int i=0; i<n; i++){
            User user = createUser(userService, company, i);
            users.add(user);
        }
        return users;
    }
}
