package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.repomapping.AutoRepoMappingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ScmRepoMappingControllerTest {
    @Mock
    OrgUsersDatabaseService orgUsersDatabaseService;

    @Mock
    ControlPlaneService controlPlaneService;

    @Mock
    InventoryService inventoryService;

    @Mock
    RedisConnectionFactory redisConnectionFactory;

    @Mock
    RedisConnection redisConnection;

    AutoRepoMappingService autoRepoMappingService;

    ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.objectMapper = DefaultObjectMapper.get();
        this.autoRepoMappingService = new AutoRepoMappingService(orgUsersDatabaseService, controlPlaneService, inventoryService, objectMapper);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(orgUsersDatabaseService.stream(anyString(), any(), any())).thenReturn(Stream.of(
                DBOrgUser.builder()
                        .ids(Set.of(DBOrgUser.LoginId.builder()
                                .cloudId("sid-propelo")
                                .build())).build()));
        when(inventoryService.getIntegration(any())).thenReturn(Integration.builder().id("testIntegrationId").satellite(false).build());
        when(redisConnection.setEx(any(), anyLong(), any())).thenReturn(true);
        when(controlPlaneService.submitJob(any())).thenReturn(SubmitJobResponse.builder().jobId("testJobId").build());
    }

    @Test
    public void testNoKeyPresent() throws SQLException, IngestionServiceException {
        when(redisConnection.exists((byte[]) any())).thenReturn(false);
        ScmRepoMappingController controller = new ScmRepoMappingController(
                autoRepoMappingService, objectMapper, redisConnectionFactory, 10
        );
        var result = controller.getRepoMappingInternal("sidTenant", "1", false);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody().getJobId()).isEqualTo("testJobId");
        assertThat(result.getBody().getResult()).isNull();
    }

    @Test
    public void testKeyPresent() throws IngestionServiceException {
        when(redisConnection.exists((byte[]) any())).thenReturn(true);
        when(redisConnection.get((byte[]) any())).thenReturn("testJobId".getBytes(StandardCharsets.UTF_8));
        when(controlPlaneService.getJob("testJobId")).thenReturn(JobDTO.builder()
                .id("testJobId")
                .status(JobStatus.SUCCESS)
                .result(objectMapper.convertValue(ScmRepoMappingResult.builder().mappedRepos(List.of("a", "b", "c")).build(), Map.class))
                .build());
        ScmRepoMappingController controller = new ScmRepoMappingController(
                autoRepoMappingService, objectMapper, redisConnectionFactory, 10
        );
        var result = controller.getRepoMappingInternal("sidTenant", "1", false);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getJobId()).isEqualTo("testJobId");
        assertThat(result.getBody().getResult()).isNotNull();
        assertThat(result.getBody().getResult().getMappedRepos()).containsExactly("a", "b", "c");
    }
}