package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.VelocityStageResult;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsPrecalculatedWidgetReadService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsWidgetService;
import io.levelops.commons.services.velocity_productivity.services.VelocityConfigsService;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@Log4j2
public class LegacyLeadTimeCalculationTest {
    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    private static VelocityAggsService velocityAggsService;
    private static VelocityAggsWidgetService velocityAggsWidgetService;
    private static VelocityConfigsService spyVelocityConfigsService;

    private static LegacyLeadTimeCalculationService legacyLeadTimeCalculationService;

    @BeforeClass
    public static void setup() {
        velocityAggsService = Mockito.mock(VelocityAggsService.class);
        AggCacheService cacheService = Mockito.mock(AggCacheService.class);
        LegacyLeadTimePrecalculatedWidgetReadService legacyLeadTimePrecalculatedWidgetReadService = Mockito.mock(LegacyLeadTimePrecalculatedWidgetReadService.class);
        velocityAggsWidgetService = Mockito.mock(VelocityAggsWidgetService.class);

        VelocityConfigsService velocityConfigsService = new VelocityConfigsService(
                Mockito.mock(VelocityConfigsDatabaseService.class), Mockito.mock(OrgProfileDatabaseService.class)
        );
        spyVelocityConfigsService = Mockito.spy(velocityConfigsService);

        legacyLeadTimeCalculationService = new LegacyLeadTimeCalculationService(
                velocityAggsService,
                mapper,
                cacheService,
                legacyLeadTimePrecalculatedWidgetReadService,
                spyVelocityConfigsService,
                velocityAggsWidgetService
        );
    }

    @Test
    public void testGetNewVelocityAggsForLeadTime() throws Exception {
        // configure
        int ouRefId = 32868;
        Map<String, Object> filters = new HashMap<>();
        filters.put("limit_to_only_applicable_data", false);
        filters.put("integration_ids", List.of("1849", "4002"));
        filters.put("ratings", List.of("good", "slow"));
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(filters)
                .ouIds(Set.of(ouRefId))
                .build();
        List<DbAggregationResult> expectedResponse = List.of(
                DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
        );
        UUID profileId = UUID.randomUUID();
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(profileId)
                .leadTimeForChanges(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stage1").build()))
                                .fixedStages(List.of(
                                        VelocityConfigDTO.Stage.builder().name("PR Creation Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Time to First Comment").build(),
                                        VelocityConfigDTO.Stage.builder().name("Approval Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Merge Time").build()
                                ))
                                .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stageLast").build()))
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .meanTimeToRestore(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of())
                                .fixedStages(List.of())
                                .postDevelopmentCustomStages(List.of())
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder().build())
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder().build())
                .build();
        doReturn(Optional.ofNullable(velocityConfigDTO)).when(spyVelocityConfigsService).getByOuRefId(any(), anyInt());
        when(velocityAggsWidgetService.getOUCustomization(eq(company), eq(originalRequest), eq("/velocity"), eq(true), any()))
                .thenReturn(VelocityAggsWidgetService.OUCustomization.builder()
                        .originalRequest(originalRequest)
                        .finalRequest(DefaultListRequest.builder().build())
                        .velocityConfigDTO(velocityConfigDTO)
                        .finalOuConfig(OUConfiguration.builder().build())
                        .build());

        when(velocityAggsService.calculateVelocity(eq(company), eq(originalRequest), any(), any(), any())).thenReturn(expectedResponse);

        //execute
        List<DbAggregationResult> response = legacyLeadTimeCalculationService.getNewVelocityAggsForLeadTime(
                company, originalRequest, false, 2L, TimeUnit.MINUTES, false
        );

        // execute
        Assert.assertEquals(expectedResponse, response);
    }


    @Test
    public void testGetNewVelocityAggsForMeanTime() throws Exception {
        // configure
        int ouRefId = 32868;
        Map<String, Object> filters = new HashMap<>();
        filters.put("limit_to_only_applicable_data", false);
        filters.put("integration_ids", List.of("1849", "4002"));
        filters.put("ratings", List.of("good", "slow"));
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(filters)
                .ouIds(Set.of(ouRefId))
                .build();
        List<DbAggregationResult> expectedResponse = List.of(
                DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
        );
        UUID profileId = UUID.randomUUID();
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(profileId)
                .leadTimeForChanges(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of())
                                .fixedStages(List.of())
                                .postDevelopmentCustomStages(List.of())
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .meanTimeToRestore(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stage1").build()))
                                .fixedStages(List.of(
                                        VelocityConfigDTO.Stage.builder().name("PR Creation Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Time to First Comment").build(),
                                        VelocityConfigDTO.Stage.builder().name("Approval Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Merge Time").build()
                                ))
                                .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stageLast").build()))
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder().build())
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder().build())
                .build();
        doReturn(Optional.ofNullable(velocityConfigDTO)).when(spyVelocityConfigsService).getByOuRefId(any(), anyInt());
        when(velocityAggsWidgetService.getOUCustomization(eq(company), eq(originalRequest), eq("/velocity"), eq(true), any()))
                .thenReturn(VelocityAggsWidgetService.OUCustomization.builder()
                        .originalRequest(originalRequest)
                        .finalRequest(DefaultListRequest.builder().build())
                        .velocityConfigDTO(velocityConfigDTO)
                        .finalOuConfig(OUConfiguration.builder().build())
                        .build());

        when(velocityAggsService.calculateVelocity(eq(company), eq(originalRequest), any(), any(), any())).thenReturn(expectedResponse);

        //execute
        List<DbAggregationResult> response = legacyLeadTimeCalculationService.getNewVelocityAggsForMeanTime(
                company, originalRequest, false, 2L, TimeUnit.MINUTES, false
        );

        // execute
        Assert.assertEquals(expectedResponse, response);
    }

    @Test
    public void testGetVelocityValuesForLeadTime() throws Exception {
        // configure
        int ouRefId = 32868;

        Map<String, Object> filters = new HashMap<>();
        filters.put("limit_to_only_applicable_data", false);
        filters.put("integration_ids", List.of("1849", "4002"));
        filters.put("ratings", List.of("good", "slow"));
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(filters)
                .ouIds(Set.of(ouRefId))
                .build();
        DbListResponse<DbAggregationResult> expectedResponse = DbListResponse.of(
                List.of(DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
                ),
                1
        );
        UUID profileId = UUID.randomUUID();
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(profileId)
                .leadTimeForChanges(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stage1").build()))
                                .fixedStages(List.of(
                                        VelocityConfigDTO.Stage.builder().name("PR Creation Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Time to First Comment").build(),
                                        VelocityConfigDTO.Stage.builder().name("Approval Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Merge Time").build()
                                ))
                                .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stageLast").build()))
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .meanTimeToRestore(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of())
                                .fixedStages(List.of())
                                .postDevelopmentCustomStages(List.of())
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder().build())
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder().build())
                .build();
        doReturn(Optional.ofNullable(velocityConfigDTO)).when(spyVelocityConfigsService).getByOuRefId(any(), anyInt());
        when(velocityAggsWidgetService.getOUCustomization(eq(company), eq(originalRequest), eq("/velocity/values"), eq(true), any()))
                .thenReturn(VelocityAggsWidgetService.OUCustomization.builder()
                        .originalRequest(originalRequest)
                        .finalRequest(DefaultListRequest.builder().build())
                        .velocityConfigDTO(velocityConfigDTO)
                        .finalOuConfig(OUConfiguration.builder().build())
                        .build());

        when(velocityAggsService.calculateVelocityValues(eq(company), eq(originalRequest), any(), any(), any())).thenReturn(expectedResponse);

        //execute
        DbListResponse<DbAggregationResult> response = legacyLeadTimeCalculationService.getVelocityValuesForLeadTime(
                company, originalRequest, false, 2L, TimeUnit.MINUTES, false
        );

        // execute
        Assert.assertEquals(expectedResponse, response);
    }

    @Test
    public void testGetVelocityValuesForMeanTime() throws Exception {
        // configure
        int ouRefId = 32868;

        Map<String, Object> filters = new HashMap<>();
        filters.put("limit_to_only_applicable_data", false);
        filters.put("integration_ids", List.of("1849", "4002"));
        filters.put("ratings", List.of("good", "slow"));
        DefaultListRequest originalRequest = DefaultListRequest.builder()
                .filter(filters)
                .ouIds(Set.of(ouRefId))
                .build();
        DbListResponse<DbAggregationResult> expectedResponse = DbListResponse.of(
                List.of(DbAggregationResult.builder()
                        .key("some-stage")
                        .count(10L)
                        .p90(80L)
                        .p95(84L)
                        .mean(5.0)
                        .median(2L)
                        .velocityStageResult(VelocityStageResult.builder()
                                .lowerLimitValue(4L)
                                .lowerLimitUnit(TimeUnit.DAYS)
                                .upperLimitValue(11L)
                                .upperLimitUnit(TimeUnit.DAYS)
                                .rating(VelocityConfigDTO.Rating.GOOD)
                                .build())
                        .build()
                ),
                1
        );
        UUID profileId = UUID.randomUUID();
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(profileId)
                .leadTimeForChanges(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of())
                                .fixedStages(List.of())
                                .postDevelopmentCustomStages(List.of())
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .meanTimeToRestore(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stage1").build()))
                                .fixedStages(List.of(
                                        VelocityConfigDTO.Stage.builder().name("PR Creation Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Time to First Comment").build(),
                                        VelocityConfigDTO.Stage.builder().name("Approval Time").build(),
                                        VelocityConfigDTO.Stage.builder().name("Merge Time").build()
                                ))
                                .postDevelopmentCustomStages(List.of(VelocityConfigDTO.Stage.builder().name("stageLast").build()))
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .startingGenericEventTypes(List.of())
                                .startingEventIsCommitCreated(true)
                                .startingEventIsGenericEvent(false)
                                .build()
                )
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder().build())
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder().build())
                .build();
        doReturn(Optional.ofNullable(velocityConfigDTO)).when(spyVelocityConfigsService).getByOuRefId(any(), anyInt());
        when(velocityAggsWidgetService.getOUCustomization(eq(company), eq(originalRequest), eq("/velocity/values"), eq(true), any()))
                .thenReturn(VelocityAggsWidgetService.OUCustomization.builder()
                        .originalRequest(originalRequest)
                        .finalRequest(DefaultListRequest.builder().build())
                        .velocityConfigDTO(velocityConfigDTO)
                        .finalOuConfig(OUConfiguration.builder().build())
                        .build());

        when(velocityAggsService.calculateVelocityValues(eq(company), eq(originalRequest), any(), any(), any())).thenReturn(expectedResponse);

        //execute
        DbListResponse<DbAggregationResult> response = legacyLeadTimeCalculationService.getVelocityValuesForMeanTime(
                company, originalRequest, false, 2L, TimeUnit.MINUTES, false
        );

        // execute
        Assert.assertEquals(expectedResponse, response);
    }
}
