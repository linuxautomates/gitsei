package io.levelops.services;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.PreflightCheck;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class PreflightCheckService {

    private static final int DEFAULT_PREFLIGHT_CHECK_TIMEOUT_IN_SECS = 30;
    private final Map<String, PreflightCheck> preflightChecksByIntegrationType = Maps.newHashMap();
    private final int preflightCheckTimeoutInSecs;

    @Autowired
    public PreflightCheckService(List<PreflightCheck> preflightChecks) {
        this(preflightChecks, null);
    }

    public PreflightCheckService(List<PreflightCheck> preflightChecks, @Nullable Integer preflightCheckTimeoutInSecs) {
        this.preflightCheckTimeoutInSecs = MoreObjects.firstNonNull(preflightCheckTimeoutInSecs, DEFAULT_PREFLIGHT_CHECK_TIMEOUT_IN_SECS);
        preflightChecks.forEach(check -> preflightChecksByIntegrationType.put(check.getIntegrationType(), check));
    }

    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheck checker = preflightChecksByIntegrationType.get(integration.getApplication());
        if (checker == null) {
            log.warn("Not preflight checker found for integration_type={}... will not run any checks.", integration.getApplication());
            return PreflightCheckResults.builder()
                    .success(true)
                    .build();
        }
        return runPreflightCheck(checker, tenantId, integration, token);
    }

    private PreflightCheckResults runPreflightCheck(PreflightCheck checker, String tenantId, Integration integration, Token token) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            return executor.submit(
                    () -> {
                        Thread.currentThread().setContextClassLoader(PreflightCheckService.class.getClassLoader());
                        return checker.check(tenantId, integration, token);
                    }
            ).get(preflightCheckTimeoutInSecs, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to run preflight check for tenant_id={} integration_type={}", tenantId, integration.getApplication(), e);
            return PreflightCheckResults.builder()
                    .success(false)
                    .exception(ExceptionUtils.getMessage(e))
                    .build();
        } finally {
            executor.shutdownNow();
        }
    }

}
