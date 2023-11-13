package io.levelops.aggregations_shared.models;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Builder(toBuilder = true)
public class SnapshottingSettings {
    private final List<IntegrationWhitelistEntry> disableSnapshottingForIntegrations;
    private final Set<String> disableSnapshottingForTenants;
    private final Set<String> deleteSnapshotDataForTenants;
    @Getter
    private final int periodicReprocessingInDays;

    public boolean isSnapshottingEnabled(String tenantId, String integrationId) {
        return !SetUtils.emptyIfNull(disableSnapshottingForTenants).contains(tenantId)
                && !ListUtils.emptyIfNull(disableSnapshottingForIntegrations).contains(IntegrationWhitelistEntry.of(tenantId, integrationId));
    }

    public boolean isDeleteSnapshotDataEnabled(String tenantId, String integrationId) {
        return !isSnapshottingEnabled(tenantId, integrationId) && SetUtils.emptyIfNull(deleteSnapshotDataForTenants).contains(tenantId);
    }

    public int getPeriodicReprocessingInMinutes() {
        return (int) TimeUnit.DAYS.toMinutes(periodicReprocessingInDays);
    }
}