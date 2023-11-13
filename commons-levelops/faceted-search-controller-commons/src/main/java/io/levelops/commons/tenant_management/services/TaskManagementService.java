package io.levelops.commons.tenant_management.services;

import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.tenant_management.models.TaskStatus;
import io.levelops.commons.tenant_management.models.TaskTracker;
import io.levelops.commons.tenant_management.models.TaskType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TaskManagementService {
    private static final boolean DID_NOT_GET_UNASSIGNED_TASK = false;

    private final TaskTrackersDBService taskTrackersDatabaseService;

    @Autowired
    public TaskManagementService(TaskTrackersDBService taskTrackersDatabaseService) {
        this.taskTrackersDatabaseService = taskTrackersDatabaseService;
    }

    public boolean getUnAssignedTask(TaskType taskType, Long schedulingIntervalInSecs) throws SQLException {
        //Insert Only ... if it did not exist...
        TaskTracker tracker = TaskTracker.builder()
                .type(taskType).frequency(schedulingIntervalInSecs.intValue()).status(TaskStatus.UNASSIGNED)
                .build();
        Optional<UUID> opt = taskTrackersDatabaseService.insertSafe(tracker);
        if (opt.isPresent()) {
            log.info("Task tracking for {} created, id {}!", tracker.getType(), opt.get());
        } else {
            log.debug("Task tracking for {} already exists!", tracker.getType());
        }

        //Read config back....
        DbListResponse<TaskTracker> dbListResponse = taskTrackersDatabaseService.list(null, 0, 100);
        if (CollectionUtils.isEmpty(dbListResponse.getRecords())) {
            return DID_NOT_GET_UNASSIGNED_TASK;
        }
        List<TaskTracker> listOfTrackers = dbListResponse.getRecords().stream().filter(g -> tracker.getType().equals(g.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(listOfTrackers)) {
            return DID_NOT_GET_UNASSIGNED_TASK;
        }
        Integer freqInSecs = listOfTrackers.get(0).getFrequency();

        //Reset Tasks based on time in config...
        boolean wereTasksReset = taskTrackersDatabaseService.updateStatusByTypeAndTimeInSeconds(tracker.getType(), freqInSecs, TaskStatus.UNASSIGNED);
        log.debug("Task tracking for {}, wereTasksReset={}!", tracker.getType(), wereTasksReset);

        //Pickup unassigned tasks
        boolean gotUnAssignedTask = taskTrackersDatabaseService.updateStatusByTypeAndStatus(tracker.getType(), TaskStatus.UNASSIGNED, TaskStatus.PENDING);
        log.debug("Task tracking for {}, gotUnAssignedTask={}!", tracker.getType(), gotUnAssignedTask);
        return gotUnAssignedTask;
    }

    public boolean updateStatusByType(final TaskType taskType, final TaskStatus status) {
        return taskTrackersDatabaseService.updateStatusByType(taskType,  status);
    }
}
