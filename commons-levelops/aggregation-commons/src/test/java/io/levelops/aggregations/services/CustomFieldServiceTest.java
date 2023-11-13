package io.levelops.aggregations.services;

import io.levelops.aggregations.models.GenericIssueManagementField;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.models.DbListResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.aggregations.services.CustomFieldService.AGGS_CUSTOM_FIELDS_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomFieldServiceTest {

    private GenericIssueManagementField createField(String name, String key) {
        return GenericIssueManagementField.builder()
                .name(name)
                .key(key)
                .build();
    }

    private IntegrationConfig.ConfigEntry createConfigEntry(String name, String key) {
        return IntegrationConfig.ConfigEntry.builder()
                .name(name)
                .key(key)
                .build();
    }

    @Test
    public void test() throws SQLException {
        IntegrationService integrationService = Mockito.mock(IntegrationService.class);
        CustomFieldService service = new CustomFieldService(integrationService);

        when(integrationService.listConfigs(anyString(), any(), any(), any())).thenReturn(
                DbListResponse.of(
                        List.of(IntegrationConfig.builder()
                                        .config(Map.of(AGGS_CUSTOM_FIELDS_KEY, List.of(
                                                createConfigEntry("junk", "randomKey"),
                                                createConfigEntry("Story Points - different", "key-1")
                                        )))
                                .build()),
                        1
                )
        );

        var fields = List.of(
                createField("Story Points", "key-1"),
                createField("Junk", "key-2"),
                createField("Sprint", "key-3"),
                createField("t-shIrt sizE", "key-4"),
                createField("random", "key-5")
        );

        service.insertPopularFieldsToIntegrationConfig(fields, "foo", "integration1");
        ArgumentCaptor<IntegrationConfig> argumentCaptor = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(integrationService, times(1)).insertConfig(any(), argumentCaptor.capture());
        var configList = argumentCaptor.getValue().getConfig().get(AGGS_CUSTOM_FIELDS_KEY);
        assertThat(configList.size()).isEqualTo(4);
        assertThat(configList.stream().map(IntegrationConfig.ConfigEntry::getKey).collect(Collectors.toSet()))
                .isEqualTo(Set.of("randomKey", "key-1", "key-3", "key-4"));

        // Test if the config is empty to begin with
        when(integrationService.listConfigs(anyString(), any(), any(), any())).thenReturn(
                DbListResponse.of(List.of(),1)
        );
        service.insertPopularFieldsToIntegrationConfig(fields, "foo", "integration1");
        argumentCaptor = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(integrationService, times(2)).insertConfig(any(), argumentCaptor.capture());
        configList = argumentCaptor.getValue().getConfig().get(AGGS_CUSTOM_FIELDS_KEY);
        assertThat(configList.size()).isEqualTo(3);
        assertThat(configList.stream().map(IntegrationConfig.ConfigEntry::getKey).collect(Collectors.toSet()))
                .isEqualTo(Set.of("key-1", "key-3", "key-4"));
    }

    @Test
    public void test1() throws SQLException {
        IntegrationService integrationService = Mockito.mock(IntegrationService.class);
        CustomFieldService service = new CustomFieldService(integrationService);

        when(integrationService.listConfigs(anyString(), any(), any(), any())).thenReturn(
                DbListResponse.of(
                        List.of(IntegrationConfig.builder()
                                .config(Map.of(AGGS_CUSTOM_FIELDS_KEY, List.of(
                                        createConfigEntry("junk", "randomKey"),
                                        createConfigEntry("Story Points - different", "key-1"),
                                        createConfigEntry("due date", "key-6")
                                )))
                                .build()),
                        1
                )
        );

        var fields = List.of(
                createField("Story Points", "key-1"),
                createField("Junk", "key-2"),
                createField("Sprint", "key-3"),
                createField("t-shIrt sizE", "key-4"),
                createField("random", "key-5"),
                createField("Due Date", "key-6")
        );

        service.insertPopularFieldsToIntegrationConfig(fields, "foo", "integration1");
        ArgumentCaptor<IntegrationConfig> argumentCaptor = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(integrationService, times(1)).insertConfig(any(), argumentCaptor.capture());
        var configList = argumentCaptor.getValue().getConfig().get(AGGS_CUSTOM_FIELDS_KEY);
        assertThat(configList.size()).isEqualTo(5);
        assertThat(configList.stream().map(IntegrationConfig.ConfigEntry::getKey).collect(Collectors.toSet()))
                .isEqualTo(Set.of("randomKey", "key-1", "key-3", "key-4","key-6"));

    }
}