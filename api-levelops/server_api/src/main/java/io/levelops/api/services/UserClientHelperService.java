package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.auth.services.SystemNotificationService;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.exceptions.EmailException;
import io.levelops.users.clients.UsersRESTClient;
import io.levelops.users.requests.ModifyUserRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
@SuppressWarnings("unused")
public class UserClientHelperService {

    private static final String ACTIVITY_LOG_TEXT = "%s User: %s.";

    private final ObjectMapper objectMapper;
    private final UsersRESTClient usersRestClient;
    private final SystemNotificationService systemNotificationService;
    private final ActivityLogService activityLogService;

    @Autowired
    public UserClientHelperService(ObjectMapper objectMapper, UsersRESTClient usersRestClient,
                                   SystemNotificationService systemNotificationService,
                                   ActivityLogService activityLogService) {
        this.objectMapper = objectMapper;
        this.usersRestClient = usersRestClient;
        this.systemNotificationService = systemNotificationService;
        this.activityLogService = activityLogService;
    }

    @SuppressWarnings("unchecked")
    public User createUser(String company, String sessionUser,
                           ModifyUserRequest request) throws IOException, EmailException, SQLException {
        Map<String, String> data = objectMapper.readValue(usersRestClient.createUser(company, request), Map.class);
        //if it reaches here then the account was created successfully as db accepted it, now send notification email
        User finalUser = usersRestClient.getUser(company, data.get("id"));
        if (Boolean.TRUE.equals(request.getNotifyUser()))
            systemNotificationService.sendAccountCreationMessage(company, finalUser, data.get("reset_token"));
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(finalUser.getId())
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.USER)
                .body(String.format(ACTIVITY_LOG_TEXT, "Created", finalUser.getId()))
                .details(Collections.singletonMap("item", finalUser))
                .action(ActivityLog.Action.CREATED)
                .build());
        return finalUser;
    }

    public Optional<User> getUserByEmail(String company, String email) throws IOException {
        return usersRestClient.listUsers(company, DefaultListRequest.builder()
                .filter(Map.of("partial", Map.of("email", email)))
                .build())
                .getRecords()
                .stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }
}
