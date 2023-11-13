package io.levelops.aggregations.services;

import io.levelops.commons.databases.models.database.dev_productivity.GlobalTracker;
import io.levelops.commons.databases.services.dev_productivity.GlobalTrackersDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.models.JobStatus;
import io.levelops.commons.models.DbListResponse;
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
public class AggTaskManagementService {
    private static final boolean DID_NOT_GET_UNASSIGNED_JOB = false;

    private final GlobalTrackersDatabaseService globalTrackersDatabaseService;

    @Autowired
    public AggTaskManagementService(GlobalTrackersDatabaseService globalTrackersDatabaseService) {
        this.globalTrackersDatabaseService = globalTrackersDatabaseService;
    }

    public boolean getUnAssignedJob(String company, String jobType, Long schedulingIntervalInSecs) throws SQLException {
        //Insert Only ... if it did not exist...
        GlobalTracker tracker = GlobalTracker.builder()
                .type(jobType).frequency(schedulingIntervalInSecs.intValue()).status(JobStatus.UNASSIGNED.toString())
                .build();
        Optional<UUID> opt = globalTrackersDatabaseService.insertSafe(company, tracker);
        if (opt.isPresent()) {
            log.info("For customer {}, global tracking for {} created, id {}!", company, tracker.getType(), opt.get());
        } else {
            log.debug("For customer {}, global tracking for {} already exists!", company, tracker.getType());
        }

        //Read config back....
        DbListResponse<GlobalTracker> dbListResponse = globalTrackersDatabaseService.list(company, 0, 10);
        if (CollectionUtils.isEmpty(dbListResponse.getRecords())) {
            return DID_NOT_GET_UNASSIGNED_JOB;
        }
        List<GlobalTracker> listOfTrackers = dbListResponse.getRecords().stream().filter(g -> tracker.getType().equals(g.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(listOfTrackers)) {
            return DID_NOT_GET_UNASSIGNED_JOB;
        }
        Integer freqInSecs = listOfTrackers.get(0).getFrequency();

        //Reset Jobs based on time in config...
        boolean wereJobsReset = globalTrackersDatabaseService.updateStatusByTypeAndTimeInSeconds(company, tracker.getType(), freqInSecs, JobStatus.UNASSIGNED.toString());
        log.debug("For customer {}, global tracking for {}, wereJobsReset={}!", company, tracker.getType(), wereJobsReset);

        //Pickup unassigned jobs
        boolean gotUnAssignedJob = globalTrackersDatabaseService.updateStatusByTypeAndStatus(company, tracker.getType(), JobStatus.UNASSIGNED.toString(), JobStatus.PENDING.toString());
        log.debug("For customer {}, global tracking for {}, gotUnAssignedJob={}!", company, tracker.getType(), gotUnAssignedJob);
        return gotUnAssignedJob;
    }

    public boolean updateStatusByType(final String company, final String type, final String status) {
        return globalTrackersDatabaseService.updateStatusByType(company, type,  status);
    }
}
