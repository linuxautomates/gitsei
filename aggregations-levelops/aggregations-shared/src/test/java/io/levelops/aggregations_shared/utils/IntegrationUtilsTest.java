package io.levelops.aggregations_shared.utils;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.models.DbListResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class IntegrationUtilsTest {

    @Test
    void isSnaphottingEnabled() {
        assertThat(IntegrationUtils.isSnaphottingEnabled(null)).isTrue();
        assertThat(IntegrationUtils.isSnaphottingEnabled(123L)).isTrue();
        assertThat(IntegrationUtils.isSnaphottingEnabled(999999999999L)).isFalse();
    }

    @Test
    void isSnaphottingDisabled() {
        assertThat(IntegrationUtils.isSnaphottingDisabled(null)).isFalse();
        assertThat(IntegrationUtils.isSnaphottingDisabled(123L)).isFalse();
        assertThat(IntegrationUtils.isSnaphottingDisabled(999999999999L)).isTrue();
    }

    @Test
    void getConfigUpdatedAt() {
        String tenantId = "foo";
        IntegrationService integrationService = Mockito.mock(IntegrationService.class);
        when(integrationService.listConfigs(eq(tenantId), eq(List.of("1")), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));
        when(integrationService.listConfigs(eq(tenantId), eq(List.of("2")), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().build())
                                .build()), 1));
        when(integrationService.listConfigs(eq(tenantId), eq(List.of("3")), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .build()), 1));
        assertThat(IntegrationUtils.getConfigUpdatedAt(integrationService, tenantId, "1", 0L)).isEqualTo(123L);
        assertThat(IntegrationUtils.getConfigUpdatedAt(integrationService, tenantId, "2", 0L)).isEqualTo(0L);
        assertThat(IntegrationUtils.getConfigUpdatedAt(integrationService, tenantId, "3", 0L)).isEqualTo(0L);
        assertThat(IntegrationUtils.getConfigUpdatedAt(integrationService, tenantId, "4", 0L)).isEqualTo(0L);

    }
}