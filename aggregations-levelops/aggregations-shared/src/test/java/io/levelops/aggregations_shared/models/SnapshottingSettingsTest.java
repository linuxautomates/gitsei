package io.levelops.aggregations_shared.models;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshottingSettingsTest {

    @Test
    public void test() {
        SnapshottingSettings settings = SnapshottingSettings.builder()
                .disableSnapshottingForTenants(Set.of("DisabledTenant"))
                .disableSnapshottingForIntegrations(List.of(
                        IntegrationWhitelistEntry.of("DisabledTenant", "1"),
                        IntegrationWhitelistEntry.of("Foo", "1"),
                        IntegrationWhitelistEntry.of("DeleteTenant", "1")
                ))
                .deleteSnapshotDataForTenants(Set.of("DeleteTenant"))
                .build();

        assertThat(settings.isSnapshottingEnabled("DisabledTenant", "1")).isFalse();
        assertThat(settings.isDeleteSnapshotDataEnabled("DisabledTenant", "1")).isFalse();
        assertThat(settings.isSnapshottingEnabled("DisabledTenant", "2")).isFalse();
        assertThat(settings.isDeleteSnapshotDataEnabled("DisabledTenant", "2")).isFalse();
        assertThat(settings.isSnapshottingEnabled("DisabledTenant", "3")).isFalse();
        assertThat(settings.isDeleteSnapshotDataEnabled("DisabledTenant", "3")).isFalse();

        assertThat(settings.isSnapshottingEnabled("Foo", "1")).isFalse();
        assertThat(settings.isDeleteSnapshotDataEnabled("Foo", "1")).isFalse();
        assertThat(settings.isSnapshottingEnabled("Foo", "2")).isTrue();
        assertThat(settings.isDeleteSnapshotDataEnabled("Foo", "2")).isFalse();
        assertThat(settings.isSnapshottingEnabled("Foo", "3")).isTrue();
        assertThat(settings.isDeleteSnapshotDataEnabled("Foo", "3")).isFalse();

        assertThat(settings.isSnapshottingEnabled("DeleteTenant", "1")).isFalse();
        assertThat(settings.isDeleteSnapshotDataEnabled("DeleteTenant", "1")).isTrue();
        assertThat(settings.isSnapshottingEnabled("DeleteTenant", "2")).isTrue();
        assertThat(settings.isDeleteSnapshotDataEnabled("DeleteTenant", "2")).isFalse();
        assertThat(settings.isSnapshottingEnabled("DeleteTenant", "3")).isTrue();
        assertThat(settings.isDeleteSnapshotDataEnabled("DeleteTenant", "3")).isFalse();
    }
}