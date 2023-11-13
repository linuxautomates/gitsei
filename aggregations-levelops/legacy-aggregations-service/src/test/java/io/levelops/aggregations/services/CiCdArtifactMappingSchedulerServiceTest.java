package io.levelops.aggregations.services;

import com.google.cloud.pubsub.v1.Publisher;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CiCdArtifactMappingSchedulerServiceTest {
    @Mock
    TenantService tenantService;

    @Mock
    Publisher publisher;

    @Mock
    AggTaskManagementService aggTaskManagementService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWhitelistEnabled() throws SQLException {
        CiCdArtifactMappingSchedulerService schedulerService = new CiCdArtifactMappingSchedulerService(
                DefaultObjectMapper.get(),
                tenantService,
                aggTaskManagementService,
                publisher,
                0L,
                0L,
                "test,test2",
                true
        );
        when(tenantService.list(anyString(), eq(0), any())).thenReturn(DbListResponse.of(
                List.of(
                        Tenant.builder()
                                .tenantName("test")
                                .id("test")
                                .build(),
                        Tenant.builder()
                                .tenantName("test2")
                                .id("test2")
                                .build(),
                        Tenant.builder()
                                .tenantName("test3")
                                .id("test3")
                                .build()
                ), 3
        ));
        when(tenantService.list(anyString(), eq(1), any())).thenReturn(DbListResponse.of(
                List.of(), 0
        ));
        when(aggTaskManagementService.getUnAssignedJob(anyString(), anyString(), any())).thenReturn(true);
        schedulerService.scheduleCicdArtifactMappingForAllTenants();
        verify(aggTaskManagementService, times(2)).getUnAssignedJob(any(), any(), any());
    }

    @Test
    public void testWhitelistDisabled() throws SQLException {
        CiCdArtifactMappingSchedulerService schedulerService = new CiCdArtifactMappingSchedulerService(
                DefaultObjectMapper.get(),
                tenantService,
                aggTaskManagementService,
                publisher,
                0L,
                0L,
                "test,test2",
               false
        );
        when(tenantService.list(anyString(), eq(0), any())).thenReturn(DbListResponse.of(
                List.of(
                        Tenant.builder()
                                .tenantName("test")
                                .id("test")
                                .build(),
                        Tenant.builder()
                                .tenantName("test2")
                                .id("test2")
                                .build(),
                        Tenant.builder()
                                .tenantName("test3")
                                .id("test3")
                                .build()
                ), 3
        ));
        when(tenantService.list(anyString(), eq(1), any())).thenReturn(DbListResponse.of(
                List.of(), 0
        ));
        when(aggTaskManagementService.getUnAssignedJob(anyString(), anyString(), any())).thenReturn(true);
        schedulerService.scheduleCicdArtifactMappingForAllTenants();
        verify(aggTaskManagementService, times(3)).getUnAssignedJob(any(), any(), any());
    }

}