package io.levelops.aggregations_shared.config;

import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.commons.utils.CommaListSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnapshottingConfig {

    @Bean
    public SnapshottingSettings snapshottingSettings(
            @Value("${DISABLE_SNAPSHOTTING_FOR_INTEGRATIONS:}") String disableSnapshottingForIntegrations,
            @Value("${DISABLE_SNAPSHOTTING_FOR_TENANTS:}") String disableSnapshottingForTenants,
            @Value("${DELETE_SNAPSHOT_DATA_FOR_TENANTS:}") String deleteSnapshotDataForTenants,
            @Value("${DISABLED_SNAPSHOTTING_PERIODIC_REPROCESSING_IN_DAYS:14}") int periodicReprocessingInDays) {
        return SnapshottingSettings.builder()
                .disableSnapshottingForIntegrations(IntegrationWhitelistEntry.fromCommaSeparatedString(disableSnapshottingForIntegrations))
                .disableSnapshottingForTenants(CommaListSplitter.splitToSet(disableSnapshottingForTenants))
                .deleteSnapshotDataForTenants(CommaListSplitter.splitToSet(deleteSnapshotDataForTenants))
                .periodicReprocessingInDays(periodicReprocessingInDays)
                .build();
    }

}
