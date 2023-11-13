package io.levelops.aggregations.services;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.TenantService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class ScmCommitPRMappingSchedulerService {
    private static final int WARMUP_DELAY_SECS = 10;

    private final ScheduledExecutorService scheduler;
    private final TenantService tenantService;
    private final TenantScmCommitPRMappingTaskScheduleService tenantScmCommitPRMappingTaskScheduleService;

    @Autowired
    public ScmCommitPRMappingSchedulerService(@Value("${SCM_COMMIT_PR_MAPPING_SCHEDULING_INTERVAL:120}") Long schedulingIntervalInSec, TenantService tenantService, TenantScmCommitPRMappingTaskScheduleService tenantScmCommitPRMappingTaskScheduleService) {
        scheduler = initScheduling(this, schedulingIntervalInSec);
        this.tenantService = tenantService;
        this.tenantScmCommitPRMappingTaskScheduleService = tenantScmCommitPRMappingTaskScheduleService;
    }

    private static ScheduledExecutorService initScheduling(ScmCommitPRMappingSchedulerService scmCommitPRMappingSchedulerService, long schedulingIntervalInSec) {
        if (schedulingIntervalInSec <= 0) {
            log.info("Scm Commit PR Mappings Scheduling is DISABLED");
            return null;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("scm-commit-pr-mapping-sched-%d")
                .build());
        executor.scheduleAtFixedRate(new ScmCommitPRMappingSchedulerService.SchedulingTask(scmCommitPRMappingSchedulerService), WARMUP_DELAY_SECS, schedulingIntervalInSec, TimeUnit.SECONDS);
        log.info("Scm Commit PR Mappings Scheduling is ENABLED (interval={}sec)", schedulingIntervalInSec);
        return executor;
    }

    public static class SchedulingTask implements Runnable {
        private final ScmCommitPRMappingSchedulerService scmCommitPRMappingService;

        public SchedulingTask(ScmCommitPRMappingSchedulerService scmCommitPRMappingService) {
            this.scmCommitPRMappingService = scmCommitPRMappingService;
        }

        @Override
        public void run() {
            boolean success = true;
            try {
                log.info("ScmCommitPRMappingSchedulerService Scheduling Job Started");
                scmCommitPRMappingService.scheduleScmCommitPRMappingForAllTenants();
            } catch (Throwable e) {
                log.warn("Failed to run Job Retrying task", e);
                success = false;
            } finally {
                log.info("ScmCommitPRMappingSchedulerService Scheduling Job Completed, success = {}", success);
            }
        }
    }

    public void scheduleScmCommitPRMappingForAllTenants() throws SQLException {
        List<Tenant> tenants =  tenantService.list("", 0, 1000).getRecords();
        log.info("ScmCommitPRMappingSchedulerService, tenants.size = {}", CollectionUtils.emptyIfNull(tenants).stream());
        if(CollectionUtils.isEmpty(tenants)) {
            return;
        }
        for(Tenant tenant : tenants) {
            tenantScmCommitPRMappingTaskScheduleService.scheduleScmCommitPRMappingForTenant(tenant.getId());
        }
    }
}
