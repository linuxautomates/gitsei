package io.levelops.api.services;

import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

@Log4j2
@Service
@SuppressWarnings("unused")
public class StateService {
    private static final String ACTIVITY_LOG_TEXT = "%s Products item: %s.";
    private final StateDBService stateDBService;
    private final ActivityLogService activityLogService;

    @Autowired
    public StateService(StateDBService stateDBService, ActivityLogService activityLogService) {
        this.stateDBService = stateDBService;
        this.activityLogService = activityLogService;
    }

    public String createState(final String company, final String sessionUser, final State state) throws SQLException {
        String stateId = stateDBService.insert(company, state);
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(stateId)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.STATE)
                .body(String.format(ACTIVITY_LOG_TEXT, "Created", stateId))
                .details(Collections.singletonMap("item", state))
                .action(ActivityLog.Action.CREATED)
                .build());
        return stateId;
    }

    public Boolean deleteState(final String company, final String sessionUser, String stateId) throws SQLException {
        Boolean deleted = stateDBService.delete(company, stateId);
        if (BooleanUtils.isTrue(deleted)) {
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(stateId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.STATE)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", stateId))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
        }
        return deleted;
    }

    public Boolean updateState(final String company, final String sessionUser, String stateId, final State state) throws SQLException {
        State sanitizedState = state.toBuilder().id(Integer.parseInt(stateId)).build();
        Boolean result = stateDBService.update(company, sanitizedState);
        if(BooleanUtils.isTrue(result)) {
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(stateId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.STATE)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited", stateId))
                    .details(Collections.singletonMap("item", sanitizedState))
                    .action(ActivityLog.Action.EDITED)
                    .build());
        }
        return result;
    }

    public Optional<State> getState(final String company, String stateId) throws SQLException {
        return stateDBService.get(company, stateId);
    }

    public PaginatedResponse<State> getStatesList(String company, String name, Integer pageNumber, Integer pageSize) throws SQLException {
        DbListResponse<State> states = stateDBService.listByFilter(company, name, null, pageNumber,pageSize);
        PaginatedResponse<State> paginatedResponse = null;
        if(CollectionUtils.isEmpty(states.getRecords())){
            paginatedResponse = PaginatedResponse.of(pageNumber, pageSize, 0, Collections.emptyList());
        } else {
            paginatedResponse = PaginatedResponse.of(pageNumber,pageSize,states.getTotalCount(),states.getRecords()
            );
        }
        return paginatedResponse;
    }
}
