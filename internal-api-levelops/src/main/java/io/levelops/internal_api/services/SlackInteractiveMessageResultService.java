package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.Notification;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import io.levelops.commons.databases.models.database.WorkItemNotification;
import io.levelops.commons.databases.services.WorkItemNotificationsDatabaseService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlackInteractiveMessageResultService {

    private final WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService;

    @Autowired
    public SlackInteractiveMessageResultService(
            WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService) {
        this.workItemNotificationsDatabaseService = workItemNotificationsDatabaseService;
    }

    public List<String> handleMessageResult(
            String company,
            SlackChatInteractiveMessageResult slackChatInteractiveMessageResult) throws SQLException {
        List<String> ids = new ArrayList<>();
        List<Notification> notifications = slackChatInteractiveMessageResult.getNotifications();
        if (CollectionUtils.isNotEmpty(notifications)) {
            List<WorkItemNotification> workItemNotifications = notifications.stream()
                    .map(x -> WorkItemNotification.builder()
                            .workItemId(x.getWorkItemId())
                            .mode(x.getMode())
                            .recipient(x.getRecipient())
                            .referenceId(x.getReferenceId())
                            .channelId(x.getChannelId())
                            .url(x.getUrl())
                            .build())
                    .collect(Collectors.toList());
            for (WorkItemNotification workItemNotification : workItemNotifications) {
                String id = workItemNotificationsDatabaseService.insert(company, workItemNotification);
                ids.add(id);
            }
        }
        return ids;
    }
}
