package io.levelops.commons.databases.services.dev_productivity.services;

import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.databases.services.dev_productivity.GlobalTrackersDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.models.JobStatus;
import io.levelops.commons.databases.services.dev_productivity.models.JobType;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Log4j2
@Service
public class DevProdTaskReschedulingService {
    private final TenantService tenantService;
    private final GlobalTrackersDatabaseService globalTrackersDatabaseService;

    @Autowired
    public DevProdTaskReschedulingService(TenantService tenantService, GlobalTrackersDatabaseService globalTrackersDatabaseService) {
        this.tenantService = tenantService;
        this.globalTrackersDatabaseService = globalTrackersDatabaseService;
    }

    private void reScheduleReport(Stream<String> tenantIds, List<String> successfulTenantIds, List<String> failedTenantIds) {
        tenantIds.forEach(t -> {
            boolean success = reScheduleReportForOneTenant(t);
            if (success) {
                successfulTenantIds.add(t);
            } else {
                failedTenantIds.add(t);
            }
        });
    }

    public List<String> reScheduleReportForMultipleTenants(List<String> tenantIds) {
        List<String> successfulTenantIds = new ArrayList<>();
        List<String> failedTenantIds = new ArrayList<>();

        Stream<String> tenantIdsStream = null;
        if (CollectionUtils.isEmpty(tenantIds)) {
            tenantIdsStream = PaginationUtils.stream(0, 1, page -> {
                try {
                    return tenantService.list("", page, 1000).getRecords();
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            }).map(Tenant::getId);
        } else {
            tenantIdsStream = tenantIds.stream();
        }

        reScheduleReport(tenantIdsStream, successfulTenantIds, failedTenantIds);
        return successfulTenantIds;
    }

    public boolean reScheduleReportForOneTenant(String tenantId) {
        boolean success = true;
        // For all Dev Prod Task Types mark job as unassigned
        for(JobType jobType : JobType.values()) {
            success = success && globalTrackersDatabaseService.updateStatusByType(tenantId, jobType.toString(), JobStatus.UNASSIGNED.toString());
        }
        return success;
    }

    public boolean reScheduleOUOrgUserMappingsForOneTenant(String tenantId) {
        return globalTrackersDatabaseService.updateStatusByType(tenantId, JobType.OU_ORG_USER_MAPPINGS.toString(), JobStatus.UNASSIGNED.toString());
    }

    public boolean reScheduleUserDevProdReportsForOneTenant(String tenantId) {
        return globalTrackersDatabaseService.updateStatusByType(tenantId, JobType.USER_DEV_PRODUCTIVITY_REPORTS.toString(), JobStatus.UNASSIGNED.toString());
    }

    public boolean reScheduleOrgDevProdReportsForOneTenant(String tenantId) {
        return globalTrackersDatabaseService.updateStatusByType(tenantId, JobType.ORG_DEV_PRODUCTIVITY_REPORTS.toString(), JobStatus.UNASSIGNED.toString());
    }
}
