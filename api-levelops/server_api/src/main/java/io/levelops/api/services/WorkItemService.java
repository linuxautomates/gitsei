package io.levelops.api.services;

import io.levelops.api.exceptions.ServerApiException;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.workitems.clients.WorkItemsClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class WorkItemService {

    private static final String ACTIVITY_LOG_TEXT = "%s Work item.";

    private final WorkItemsClient workItemClient;
    private final ActivityLogService activityLogService;

    @Autowired
    public WorkItemService(WorkItemsClient workItemClient, ActivityLogService activityLogService) {
        this.workItemClient = workItemClient;
        this.activityLogService = activityLogService;
    }

    public WorkItem updateWorkItem(final String company,
                                   String sessionUser,
                                   String userType,
                                   final UUID id,
                                   final WorkItem workitem) throws InternalApiClientException, SQLException {
        WorkItem item = workItemClient.getById(company, id);
        if (RoleType.fromString(userType) == RoleType.ASSIGNED_ISSUES_USER) {
            if (item == null) {
                throw new ServerApiException(HttpStatus.NOT_FOUND, "Ticket not found.");
            }

            List<WorkItem.Assignee> assigneeList = item.getAssignees();
            if (assigneeList == null || assigneeList.stream()
                    .filter(assignee -> StringUtils.equals(assignee.getUserEmail(), sessionUser))
                    .findFirst()
                    .isEmpty()) {
                throw new ServerApiException(HttpStatus.FORBIDDEN, "Insufficient permissions.");
            }
        }
        String status = workitem.getStatus() != null ? workitem.getStatus() : item.getStatus();
        workItemClient.update(company, sessionUser, workitem.toBuilder().id(id.toString()).status(status).build());
        WorkItem updated = workItemClient.getById(company, id);
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(id.toString())
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.TICKET)
                .body(String.format(ACTIVITY_LOG_TEXT, "Edited"))
                .details(Collections.singletonMap("item", updated))
                .action(ActivityLog.Action.EDITED)
                .build());
        return updated;
    }
}
