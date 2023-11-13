package io.levelops.aggregations_shared.utils;

import com.google.common.base.Stopwatch;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.services.IntegrationService;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
public class IntegrationUtils {

    public static final long DISABLE_SNAPSHOTTING = 999999999999L; // Arbitrary value lower than Postgres timestamp's max value (which is around year 294276, note that MAX_LONG is a lot larger)

    public static boolean isSnaphottingDisabled(Long aggregatedAt) {
        return aggregatedAt != null && aggregatedAt == DISABLE_SNAPSHOTTING;
    }

    public static boolean isSnaphottingEnabled(Long aggregatedAt) {
        return !isSnaphottingDisabled(aggregatedAt);
    }

    public static Long getConfigUpdatedAt(IntegrationService integrationService, String tenantId, String integrationId, Long defaultValue) {
        log.debug("Entering getConfigUpdatedAt for tenantId={}, integrationId={}", tenantId, integrationId);
        Stopwatch stopwatch = log.isDebugEnabled() ? Stopwatch.createStarted() : null;
        try {
            return integrationService.listConfigs(tenantId, List.of(integrationId), 0, 1)
                    .getRecords()
                    .stream()
                    .findFirst()
                    .map(IntegrationConfig::getMetadata)
                    .map(IntegrationConfig.Metadata::getConfigUpdatedAt)
                    .orElse(defaultValue);
        } catch (Exception e) {
            log.warn("Failed to get config updated at for tenantId={}, integrationId={}, defaulting to {}", tenantId, integrationId, defaultValue);
            return defaultValue;
        } finally {
            long elapsedMs = stopwatch != null ? stopwatch.elapsed(TimeUnit.MILLISECONDS) : -1;
            log.debug("Exiting getConfigUpdatedAt for tenantId={}, integrationId={}, elapsed={}ms", tenantId, integrationId, elapsedMs);
        }

    }

}
