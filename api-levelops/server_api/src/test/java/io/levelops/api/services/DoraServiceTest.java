package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.controllers.ConfigTableHelper;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraDrillDownDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.service.dora.ADODoraService;
import io.levelops.commons.service.dora.CiCdDoraService;
import io.levelops.commons.service.dora.JiraDoraService;
import io.levelops.commons.service.dora.ScmDoraService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.commons.services.velocity_productivity.services.VelocityConfigsService;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import io.levelops.faceted_search.services.workitems.EsJiraIssueQueryService;
import io.levelops.faceted_search.services.workitems.EsWorkItemsQueryService;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class DoraServiceTest {

    private static final String company = "test";

    @Autowired
    private JiraFilterParser jiraFilterParser;
    @Autowired
    private JiraDoraService jiraDoraService;
    @Autowired
    private OrgUnitHelper orgUnitHelper;
    @Autowired
    private ScmDoraService scmDoraService;
    @Autowired
    private AggCacheService aggCacheService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VelocityConfigsService velocityConfigsService;
    @Autowired
    private VelocityAggsService velocityAggsService;
    @Autowired
    private ADODoraService adoDoraService;
    @Autowired
    CiCdAggsService ciCdAggsService;
    @Autowired
    ConfigTableHelper configTableHelper;

    private DoraService doraService;

    @Autowired
    private JiraIssueService jiraIssueService;

    @Autowired
    private EsJiraIssueQueryService esJiraIssueQueryService;


    @Autowired
    IntegrationService integrationService;

    @Autowired
    WorkItemsService workItemsService;

    @Autowired
    EsWorkItemsQueryService esWorkItemsQueryService;

    @Autowired
    IngestedAtCachingService ingestedAtCachingService;

    @Autowired
    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @Autowired
    EsScmPRsService esScmPRsService;

    @Autowired
    ScmAggService scmAggService;

    @Autowired
    CiCdDoraService ciCdDoraService;

    @Before
    public void setup() {
        doraService = new DoraService(
                jiraFilterParser,
                jiraDoraService,
                adoDoraService,
                orgUnitHelper,
                scmDoraService,
                aggCacheService,
                velocityConfigsService,
                jiraIssueService,
                integrationService,
                objectMapper,
                ciCdAggsService,
                ciCdDoraService);
    }

    @Test
    public void testGetListForCICD() throws Exception {
        Boolean disableCache = true;
        int dummyOuRefId = 13;
        DefaultListRequest dummyRequest = DefaultListRequest.builder()
                .filter(new HashMap<>(Map.of("time_range", Map.of("$gt", 12345, "$lt", 78965))))
                .ouIds(Set.of(dummyOuRefId))
                .widget(doraService.DEPLOYMENT_FREQUENCY)
                .build();
        int integrationId = 1;
        String application = "CICD";

        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType(application)
                                        .event(VelocityConfigDTO.Event.builder().values(Arrays.asList("e0a70eae-f092-466f-9d6a-fc950f843db5",
                                                "b70eb745-2d6e-4f7d-a6a1-bd42311ad82f",
                                                "0ab2b444-66cc-4041-b75d-503878b362eb")).build())
                                        .calculationField(VelocityConfigDTO.CalculationField.end_time)
                                        .build())
                                .build())
                        .build()).associatedOURefIds(Arrays.asList(String.valueOf(dummyOuRefId))).build();
        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder()
                        .application(application)
                        .build()));
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(dummyRequest).build());
        CICDJobRunDTO cicdJobRunDTO1 = CICDJobRunDTO.builder().cicdJobId(UUID.fromString("e0a70eae-f092-466f-9d6a-fc950f843db5")).build();
        CICDJobRunDTO cicdJobRunDTO2 = CICDJobRunDTO.builder().cicdJobId(UUID.fromString("b70eb745-2d6e-4f7d-a6a1-bd42311ad82f")).build();
        CICDJobRunDTO cicdJobRunDTO3 = CICDJobRunDTO.builder().cicdJobId(UUID.fromString("0ab2b444-66cc-4041-b75d-503878b362eb")).build();
        List<CICDJobRunDTO> list =new ArrayList<CICDJobRunDTO>();
        list.add(cicdJobRunDTO1);
        list.add(cicdJobRunDTO2);
        list.add(cicdJobRunDTO3);
        when(ciCdAggsService.listCiCdJobRunsForDora(any(), any(), any(), any(), any(), any())).thenReturn(
                DbListResponse.of(list, 3));

        DbListResponse<DoraDrillDownDTO>  dbListResponse = doraService.getList(disableCache, company,dummyRequest, velocityConfigDTO);

        Assert.assertEquals(dbListResponse.getRecords().get(0).getCicdJobId(), list.get(0).getCicdJobId());
        Assert.assertEquals(dbListResponse.getRecords().get(1).getCicdJobId(), list.get(1).getCicdJobId());
        Assert.assertEquals(dbListResponse.getRecords().get(2).getCicdJobId(), list.get(2).getCicdJobId());
    }

    @Test
    public void testGenerateDeploymentFrequencyForSCM() throws Exception {
        int integrationId = 1;
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .filter(Map.of("status_categories", "Done"))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .calculationField(VelocityConfigDTO.CalculationField.issue_resolved_at)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of("statues","DONE"))
                                        .build())
                                .build())
                        .build()).build();
        DoraResponseDTO expectedResponse = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder()
                        .day(List.of(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(123456L)
                                .additionalKey("23-11-2022")
                                .count(1)
                                .build()))
                        .week(List.of(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(234567L)
                                .additionalKey("18-11-2022")
                                .count(1)
                                .build()))
                        .month(List.of(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(345678L)
                                .additionalKey("1-11-2022")
                                .count(1)
                                .build()))
                        .build())
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(1)
                        .build())
                .build();

        when(scmDoraService.calculateDeploymentFrequency(any(), any(), any(), any(), any()))
                .thenReturn(expectedResponse);
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());

        DoraResponseDTO response = doraService.generateDeploymentFrequencyForSCM(company, defaultListRequest, false, velocityConfigDTO);
        Assert.assertEquals(expectedResponse.getStats().getTotalDeployment(), response.getStats().getTotalDeployment());
        Assert.assertEquals(expectedResponse.getTimeSeries().getDay().get(0).getCount(), response.getTimeSeries().getDay().get(0).getCount());
    }

    @Test
    public void testGenerateChangeFailureRateForSCM() throws Exception {

        int integrationId = 1;
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .filter(Map.of("status_categories", "Done"))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(integrationId)
                        .calculationField(VelocityConfigDTO.CalculationField.issue_resolved_at)
                        .isAbsoulte(false)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .totalDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of())
                                        .build())
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of())
                                        .build())
                                .build())
                        .build())
                .build();
        DoraResponseDTO expectedResponse = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder()
                        .day(List.of(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(123456L)
                                .additionalKey("7-11-2022")
                                .count(1)
                                .build()))
                        .week(List.of(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(234567L)
                                .additionalKey("4-11-2022")
                                .count(1)
                                .build()))
                        .month(List.of(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(345678L)
                                .additionalKey("1-11-2022")
                                .count(1)
                                .build()))
                        .build())
                .stats(DoraSingleStateDTO.builder()
                        .band(DoraSingleStateDTO.Band.LOW)
                        .countPerDay(3.5)
                        .failureRate(15.6)
                        .totalDeployment(1)
                        .build())
                .build();

        when(scmDoraService.calculateChangeFailureRate(any(), any(), any(), any(), any()))
                .thenReturn(expectedResponse);
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());

        DoraResponseDTO response = doraService.generateChangeFailureRateForSCM(company, defaultListRequest, false, velocityConfigDTO);
        Assert.assertEquals(expectedResponse.getTimeSeries().getDay().get(0).getCount(), response.getTimeSeries().getDay().get(0).getCount());
        Assert.assertEquals(expectedResponse.getStats().getBand(), response.getStats().getBand());
        Assert.assertEquals(expectedResponse.getStats().getCountPerDay(), response.getStats().getCountPerDay());
        Assert.assertEquals(expectedResponse.getStats().getFailureRate(), response.getStats().getFailureRate());
    }

    @Test
    public void testGenerateDFCountForIM_ForJIRA() throws Exception {

        int integrationId = 1;
        String application = "JIRA";

        DoraResponseDTO response = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder().build())
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(3)
                        .countPerDay((double)3/6)
                        .band(DoraSingleStateDTO.Band.HIGH)
                        .build())
                .build();
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .filter(Map.of("status_categories", "Done"))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_resolved_at)
                                        .filter(Map.of("statues","DONE"))
                                        .build())
                                .build())
                        .build()).build();

        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder().application(application).build()));
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(jiraFilterParser.createFilter((String) any(), (DefaultListRequest) any(),
                (JiraIssuesFilter.CALCULATION) any(), (JiraIssuesFilter.DISTINCT) any(),
                (String) any(), (String) any(), nullable(Boolean.class), nullable(Boolean.class)))
                .thenReturn(JiraIssuesFilter.builder()
                        .issueResolutionRange(ImmutablePair.of(1668211200L, 1668729599L))
                        .build());
        when(jiraDoraService.getTimeSeriesDataForDeployment(anyString(), any(), any(), anyString(), any())).thenReturn(response);

        DoraResponseDTO doraResponseDTO = doraService.generateDFCountForIM(company, defaultListRequest, velocityConfigDTO);

        Assert.assertEquals(doraResponseDTO.getStats().getBand(), response.getStats().getBand());
        Assert.assertEquals(doraResponseDTO.getStats().getCountPerDay(), response.getStats().getCountPerDay());
    }

    @Test
    public void testGenerateDFCountForIM_ForJIRA_workItemUpdatedAt() throws Exception {

        int integrationId = 1;
        String application = "JIRA";

        DoraResponseDTO response = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder().build())
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(3)
                        .countPerDay((double)3/6)
                        .band(DoraSingleStateDTO.Band.HIGH)
                        .build())
                .build();
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .filter(Map.of("status_categories", "Done"))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_updated_at)
                                        .filter(Map.of("statues","DONE"))
                                        .build())
                                .build())
                        .build()).build();

        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder().application(application).build()));
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(jiraFilterParser.createFilter((String) any(), (DefaultListRequest) any(),
                (JiraIssuesFilter.CALCULATION) any(), (JiraIssuesFilter.DISTINCT) any(),
                (String) any(), (String) any(), nullable(Boolean.class), nullable(Boolean.class)))
                .thenReturn(JiraIssuesFilter.builder()
                        .issueResolutionRange(ImmutablePair.of(1668211200L, 1668729599L))
                        .build());
        when(jiraDoraService.getTimeSeriesDataForDeployment(anyString(), any(), any(), anyString(), any())).thenReturn(response);

        DoraResponseDTO doraResponseDTO = doraService.generateDFCountForIM(company, defaultListRequest, velocityConfigDTO);

        Assert.assertEquals(doraResponseDTO.getStats().getBand(), response.getStats().getBand());
        Assert.assertEquals(doraResponseDTO.getStats().getCountPerDay(), response.getStats().getCountPerDay());
    }

    @Test
    public void testGenerateDFCountForIM_ForJIRA_issueReleasedIn() throws Exception {

        int integrationId = 1;
        String application = "JIRA";

        DoraResponseDTO response = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder().build())
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(3)
                        .countPerDay((double)3/6)
                        .band(DoraSingleStateDTO.Band.HIGH)
                        .build())
                .build();
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .filter(Map.of("status_categories", "Done"))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .calculationField(VelocityConfigDTO.CalculationField.released_in)
                                        .filter(Map.of("statues","DONE"))
                                        .build())
                                .build())
                        .build()).build();

        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder().application(application).build()));
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(jiraFilterParser.createFilter((String) any(), (DefaultListRequest) any(),
                (JiraIssuesFilter.CALCULATION) any(), (JiraIssuesFilter.DISTINCT) any(),
                (String) any(), (String) any(), nullable(Boolean.class), nullable(Boolean.class)))
                .thenReturn(JiraIssuesFilter.builder()
                        .issueResolutionRange(ImmutablePair.of(1668211200L, 1668729599L))
                        .build());
        when(jiraDoraService.getTimeSeriesDataForDeployment(anyString(), any(), any(), anyString(), any())).thenReturn(response);

        DoraResponseDTO doraResponseDTO = doraService.generateDFCountForIM(company, defaultListRequest, velocityConfigDTO);

        Assert.assertEquals(doraResponseDTO.getStats().getBand(), response.getStats().getBand());
        Assert.assertEquals(doraResponseDTO.getStats().getCountPerDay(), response.getStats().getCountPerDay());
    }

    @Test
    public void testGenerateCFRCountForIM_ForJIRA() throws SQLException, InventoryException, BadRequestException {

        int integrationId = 1;
        String application = "JIRA";

        DoraResponseDTO  response = DoraResponseDTO.builder()
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(5)
                        .build())
                .build();
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .isAbsoulte(false)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .totalDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of())
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_resolved_at)
                                        .build())
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of())
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_updated_at)
                                        .build())
                                .build())
                        .build())
                .build();

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder().application(application).build()));
        when(jiraFilterParser.createFilter((String) any(), (DefaultListRequest) any(),
                (JiraIssuesFilter.CALCULATION) any(), (JiraIssuesFilter.DISTINCT) any(),
                (String) any(), (String) any(), nullable(Boolean.class), nullable(Boolean.class)))
                .thenReturn(JiraIssuesFilter.builder()
                        .issueResolutionRange(ImmutablePair.of(1668211200L, 1668729599L))
                        .build());
        when(jiraDoraService.getTimeSeriesDataForDeployment(any(), any(), any(), anyString(), any()))
                .thenReturn(response);
        when(jiraDoraService.getCountForDeployment(anyString(), any(JiraIssuesFilter.class), any(JiraIssuesFilter.class)))
                .thenReturn(10L);

        DoraResponseDTO doraResponseDTO = doraService.generateCFRCountForIM(company, defaultListRequest, velocityConfigDTO);

        Assert.assertNotNull(doraResponseDTO.getStats().getBand());
        Assert.assertNotNull(doraResponseDTO.getStats().getFailureRate());
        Assert.assertEquals((Double)100.0, doraResponseDTO.getStats().getFailureRate());
        Assert.assertEquals(DoraSingleStateDTO.Band.LOW, doraResponseDTO.getStats().getBand());
    }

    @Test
    public void testGenerateDFCountForIM_ForADO() throws SQLException, InventoryException, BadRequestException {

        int integrationId = 1;
        String application = "AZURE_DEVOPS";

        DoraResponseDTO response = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder().build())
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(3)
                        .countPerDay((double)3/13)
                        .band(DoraSingleStateDTO.Band.HIGH)
                        .build())
                .build();
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .filter(Map.of("workitem_resolved_at",
                        Map.of("$gt","1668470400", "$lt", "1669597799")))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of("statues","DONE"))
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_resolved_at)
                                        .build())
                                .build())
                        .build()).build();
        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder().application(application).build()));
        when(adoDoraService.getTimeSeriesDataForDeployment(anyString(),
                any(), any(), any(), any(), any(), anyString())).thenReturn(response);

        DoraResponseDTO doraResponseDTO = doraService.generateDFCountForIM(company, defaultListRequest, velocityConfigDTO);

        Assert.assertEquals(doraResponseDTO.getStats().getBand(), response.getStats().getBand());
        Assert.assertEquals(doraResponseDTO.getStats().getCountPerDay(), response.getStats().getCountPerDay());

    }

    @Test
    public void testGenerateCFRCountForIM_ForADO() throws SQLException, InventoryException, BadRequestException {

        int integrationId = 1;
        String application = "AZURE_DEVOPS";

        DoraResponseDTO response = DoraResponseDTO.builder()
                .stats(DoraSingleStateDTO.builder()
                        .totalDeployment(25)
                        .build())
                .build();
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .isAbsoulte(false)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .totalDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .filter(Map.of())
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_resolved_at)
                                        .build())
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .calculationField(VelocityConfigDTO.CalculationField.issue_updated_at)
                                        .filter(Map.of())
                                        .build())
                                .build())
                        .build())
                .build();

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(integrationService.get(anyString(), anyString()))
                .thenReturn(Optional.of(Integration.builder().application(application).build()));
        when(adoDoraService.getTimeSeriesDataForDeployment(anyString(), any(), any(), any(), any(), any(), anyString()))
                .thenReturn(response);

        DoraResponseDTO doraResponseDTO = doraService.generateCFRCountForIM(company, defaultListRequest, velocityConfigDTO);

        Assert.assertNotNull(doraResponseDTO.getStats().getBand());
        Assert.assertNotNull(doraResponseDTO.getStats().getFailureRate());
        Assert.assertEquals((Double)100.0, doraResponseDTO.getStats().getFailureRate());
        Assert.assertEquals(DoraSingleStateDTO.Band.LOW, doraResponseDTO.getStats().getBand());
    }

    @Test
    public void testGetVelocityConfigByOuWithoutOuIds() throws SQLException {
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .ouIds(Set.of())
                .build();
        when(velocityConfigsService.getByOuRefId(anyString(), anyInt())).thenReturn(Optional.ofNullable(VelocityConfigDTO.builder().build()));
        Assert.assertThrows(RuntimeException.class, () ->{
            doraService.getVelocityConfigByOu(company, defaultListRequest);
        });
    }

    @Test
    public void testGetVelocityConfigByOuWithoutVelocityConfig() throws SQLException {
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .ouIds(Set.of(12))
                .build();
        when(velocityConfigsService.getByOuRefId(anyString(), anyInt())).thenReturn(Optional.empty());
        Assert.assertThrows(RuntimeException.class, () ->{
            doraService.getVelocityConfigByOu(company, defaultListRequest);
        });
    }


    @Test
    public void testGenerateDFCountForCICD() throws SQLException, BadRequestException {
        // Configure
        int dummyOuRefId = 13;
        DefaultListRequest dummyRequest = DefaultListRequest.builder()
                .filter(Map.of("end_time", Map.of("$gt", "12345", "$lt", "78965"),
                        "job_normalized_full_names", List.of("github/shipt/neutron/android")))
                .ouIds(Set.of(dummyOuRefId))
                .build();
        VelocityConfigDTO dummyVelocityConfigDto = VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .calculationField(VelocityConfigDTO.CalculationField.end_time)
                                        .event(VelocityConfigDTO.Event.builder()
                                                .params(new HashMap<>(
                                                        Map.of("MAJOR_VERSION", List.of("v0.1."),
                                                                "BRANCH", List.of("MASTER"))
                                                ))
                                                .build())
                                        .filter(new HashMap<>(Map.of("job_normalized_full_names", List.of("github/shipt/neutron/test"))))
                                        .isCiJob(false)
                                        .isCdJob(true)
                                        .build())
                                .build())
                        .build())
                .build();
        OUConfiguration ouConfiguration = OUConfiguration.builder()
                .ouId(UUID.randomUUID())
                .ouRefId(dummyOuRefId)
                .request(dummyRequest)
                .filters(dummyRequest.getFilter())
                .build();

        when(orgUnitHelper.getOuConfigurationFromRequest(
                eq(company), anySet(), eq(dummyRequest)
        )).thenReturn(ouConfiguration);

        DoraResponseDTO expectedResponse = DoraResponseDTO.builder()
                .timeSeries(DoraTimeSeriesDTO.builder()
                        .day(Collections.singletonList(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(123455L)
                                .count(12)
                                .additionalKey("10-11-2022")
                                .build()))
                        .week(Collections.singletonList(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(123455L)
                                .count(12)
                                .additionalKey("01-11-2022")
                                .build()))
                        .month(Collections.singletonList(DoraTimeSeriesDTO.TimeSeriesData.builder()
                                .key(123455L)
                                .count(12)
                                .additionalKey("10-11-2022")
                                .build()))
                        .build())
                .stats(
                        DoraSingleStateDTO.builder()
                                .countPerDay(1.112233)
                                .band(DoraSingleStateDTO.Band.HIGH)
                                .totalDeployment(12)
                                .build()
                ).build();
        CiCdJobRunsFilter expectedFilter = cicdJobRunFilter().toBuilder()
                .jobNormalizedFullNames(List.of("github/shipt/neutron/android", "github/shipt/neutron/test"))
                .startTimeRange(ImmutablePair.nullPair())
                .parameters(List.of(CiCdJobRunParameter.builder().name("BRANCH").values(List.of("MASTER")).build(), CiCdJobRunParameter.builder()
                        .name("MAJOR_VERSION")
                        .values(List.of("v0.1."))
                        .build()))
                .build();
        when(ciCdDoraService.calculateNewDeploymentFrequency(anyString(), any(), any(), any(), any()))
                .thenReturn(expectedResponse);

        // Execute
        DoraResponseDTO actualResponse = doraService.generateDFCountForCICD(
                company,
                dummyRequest,
                dummyVelocityConfigDto
        );

        // Assert
        verify(ciCdDoraService, times(1)).calculateNewDeploymentFrequency(eq(company), eq(expectedFilter), eq(ouConfiguration), eq(dummyVelocityConfigDto), any());
        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testScmPRDrillDownForDF() throws Exception {

        int integrationId = 1;
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .widget(scmDoraService.DEPLOYMENT_FREQUENCY)
                .filter(new HashMap<>(Map.of("integration_ids", List.of(integrationId),"time_range", Map.of("$gt", "12345", "$lt", "78965"))))
                .build();
        VelocityConfigDTO velocityConfigDTO =  VelocityConfigDTO.builder()
                .associatedOURefIds(List.of("14924", "18092"))
                .isNew(true)
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType("SCM")
                                        .filter(Map.of("statues","DONE"))
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .build())
                                .build())
                        .build())
                .build();

        String scmPrs = ResourceUtils.getResourceAsString("dora/dora_scm_prs.json");
        PaginatedResponse<DbScmPullRequest> results = objectMapper.readValue(scmPrs,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbScmPullRequest.class));

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(defaultListRequest).build());
        when(integrationService.get(company, String.valueOf(integrationId)))
                .thenReturn(Optional.ofNullable(Integration.builder().application("Github").build()));
        when(scmDoraService.getPrBasedDrillDownData(anyString(), any(), any(), any(), anyInt(), anyInt(), any(), anyString()))
                .thenReturn(DbListResponse.of(results.getResponse().getRecords(), results.getResponse().getCount()));

        DbListResponse<DoraDrillDownDTO> doraDrilldownSCM = doraService.getScmDrillDownList(true, company,
                defaultListRequest, velocityConfigDTO);

        DefaultObjectMapper.prettyPrint(doraDrilldownSCM);
        Assert.assertEquals(3, (int) doraDrilldownSCM.getCount());
        doraDrilldownSCM.getRecords().forEach(entry -> {
            Assert.assertEquals("hf", entry.getSourceBranch());
            Assert.assertEquals("PEER_REVIEWED", entry.getReviewType());
            Assert.assertEquals("levelops/commons-levelops", entry.getProject());
            Assert.assertNotNull(entry.getPrMergedAt());
        });
    }

    @Test
    public void testScmDirectCommitDrillDownForCFR() throws Exception {

        DefaultListRequest request = DefaultListRequest.builder()
                .filter(new HashMap<>(Map.of("time_range", Map.of("$gt", "1664582400", "$lt", "1669852800"), "across", "velocity")))
                .page(0)
                .pageSize(10)
                .ouIds(Set.of(32110))
                .widget(scmDoraService.CHANGE_FAILURE_RATE)
                .widgetId("e3a13490-967b-11ed-b20d-ed5e0e447b12")
                .build();

        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .isNew(true)
                .associatedOURefIds(List.of("32110"))
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(1)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("hotfix")))))
                                        .integrationType("SCM")
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .build())
                                .totalDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("main")))))
                                        .integrationType("SCM")
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .build())
                                .build())
                        .build())
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationIds(List.of(1))
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("main")))))
                                        .integrationType("SCM")
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.commit_merged_to_branch)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.commit)
                                        .calculationField(VelocityConfigDTO.CalculationField.committed_at)
                                        .build())
                                .build())
                        .build())
                .build();

        String scmCommits = ResourceUtils.getResourceAsString("dora/dora_scm_commits.json");
        PaginatedResponse<DbScmCommit> results = objectMapper.readValue(scmCommits,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbScmCommit.class));
        List<DbScmCommit> scmCommitForCFR = results.getResponse().getRecords().stream().filter(scmCommit -> "hotfix".equals(scmCommit.getBranch())).collect(Collectors.toList());

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(request).build());
        when(scmDoraService.getCommitBasedDrillDownData(anyString(), any(ScmCommitFilter.class), any(OUConfiguration.class), anyMap(), anyInt(), anyInt(), anyMap(), anyString(), anyString()))
                .thenReturn(DbListResponse.of(scmCommitForCFR, scmCommitForCFR.size()));

        DbListResponse<DoraDrillDownDTO> scmCommitList = doraService.getScmDrillDownList(false,
                "test",
                request,
                velocityConfigDTOForSCM);

        DefaultObjectMapper.prettyPrint(scmCommitList);
        Assert.assertEquals(6, (int) scmCommitList.getCount());
        scmCommitList.getRecords().forEach(entry -> {
            Assert.assertEquals("hotfix", entry.getBranch());
            Assert.assertTrue((entry.getCommitPushedAt() > 1664582400L) && (entry.getCommitPushedAt() < 1669852800L));
        });
    }

    @Test
    public void testGetScmCommitListForDF() throws Exception {

        DefaultListRequest request = DefaultListRequest.builder()
                .filter(new HashMap<>(Map.of("time_range", Map.of("$gt", "1664582400", "$lt", "1669852800"), "across", "velocity")))
                .page(0)
                .pageSize(10)
                .ouIds(Set.of(32110))
                .widget(scmDoraService.DEPLOYMENT_FREQUENCY)
                .widgetId("e3a13490-967b-11ed-b20d-ed5e0e447b12")
                .build();

        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .associatedOURefIds(List.of("32110"))
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(1)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .build())
                                .build())
                        .build())
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(1)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("main")))))
                                        .integrationType("SCM")
                                        .build())
                                .build())
                        .build())
                .build();

        String scmCommits = ResourceUtils.getResourceAsString("dora/dora_scm_commits.json");
        PaginatedResponse<DbScmCommit> results = objectMapper.readValue(scmCommits,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbScmCommit.class));
        List<DbScmCommit> scmCommitForDF = results.getResponse().getRecords().stream().filter(scmCommit -> "main".equals(scmCommit.getBranch())).collect(Collectors.toList());

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(request).build());
        when(scmDoraService.getScmCommitDrillDownData(anyString(), any(ScmCommitFilter.class), any(OUConfiguration.class), any(),
                anyInt(), anyInt(), anyMap())).thenReturn(DbListResponse.of(scmCommitForDF, scmCommitForDF.size()));

        DbListResponse<DoraDrillDownDTO> scmCommitList = doraService.getScmCommitList(false,
                "test",
                request,
                velocityConfigDTOForSCM);

        DefaultObjectMapper.prettyPrint(scmCommitList);
        Assert.assertEquals(3, (int) scmCommitList.getCount());
        scmCommitList.getRecords().forEach(entry -> {
            Assert.assertEquals("main", entry.getBranch());
            Assert.assertTrue((entry.getCommitPushedAt() > 1664582400L) && (entry.getCommitPushedAt() < 1669852800L));
        });
    }

    @Test
    public void testBandCalculationForCFR(){
        Assert.assertEquals(DoraSingleStateDTO.Band.ELITE, DoraCalculationUtils.calculateChangeFailureRateBand(0));
        Assert.assertEquals(DoraSingleStateDTO.Band.ELITE, DoraCalculationUtils.calculateChangeFailureRateBand(15));
        Assert.assertEquals(DoraSingleStateDTO.Band.HIGH, DoraCalculationUtils.calculateChangeFailureRateBand(15.1));
        Assert.assertEquals(DoraSingleStateDTO.Band.HIGH, DoraCalculationUtils.calculateChangeFailureRateBand(30));
        Assert.assertEquals(DoraSingleStateDTO.Band.MEDIUM, DoraCalculationUtils.calculateChangeFailureRateBand(30.1));
        Assert.assertEquals(DoraSingleStateDTO.Band.MEDIUM, DoraCalculationUtils.calculateChangeFailureRateBand(45));
        Assert.assertEquals(DoraSingleStateDTO.Band.LOW, DoraCalculationUtils.calculateChangeFailureRateBand(45.1));
        Assert.assertEquals(DoraSingleStateDTO.Band.LOW, DoraCalculationUtils.calculateChangeFailureRateBand(100));
    }

    @Test
    public void testGetCicdJobParams() throws Exception {
        // configure
        List<String> jobIds = new ArrayList<>() {{
            add("a3e5d742-bc8f-420d-9dee-d8f76e45ac45");
            add("bfa6e6a3-5359-4196-806d-090f574d8f9e");
            add("30ca3cab-903f-4fb3-b60e-7edca4a0ec53");
        }};
        when(ciCdDoraService.getCicdJobParams("test", jobIds)).thenReturn(
                List.of(
                        Map.of("name", "nameA", "value", "valueA1"),
                        Map.of("name", "nameA", "value", "valueA2"),
                        Map.of("name", "nameB", "value", "valueB1"),
                        Map.of("name", "nameB", "value", "valueB2"),
                        Map.of("name", "nameB", "value", "valueB3"),
                        Map.of("name", "nameC", "value", "valueC1")
                )
        );

        Map<String, List<String>> expectedResult = Map.of(
                "nameA", List.of("valueA1", "valueA2"),
                "nameB", List.of("valueB1", "valueB2", "valueB3"),
                "nameC", List.of("valueC1")
        );

        // execute
        Map<String, List<String>> result = doraService.getCicdJobParams("test", jobIds);

        // assert
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testGenerateCFRCountforCICD() throws BadRequestException, SQLException {
        // configure
        int dummyOuRefId = 13;
        DefaultListRequest dummyRequest = DefaultListRequest.builder()
                .filter(new HashMap<>(
                        Map.of(
                                "time_range", Map.of("$gt", 12345, "$lt", 78965),
                                "start_time", Map.of("$gt", 12345, "$lt", 78965),
                                "end_time", Map.of("$gt", 12345, "$lt", 78965)
                        )
                ))
                .ouIds(Set.of(dummyOuRefId))
                .build();

        int integrationId = 1;
        String application = "harnessng";
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(integrationId)
                        .integrationIds(List.of(integrationId))
                        .isAbsoulte(false)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .totalDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType(application)
                                        .event(VelocityConfigDTO.Event.builder().values(Arrays.asList("e0a70eae-f092-466f-9d6a-fc950f843db5",
                                                "b70eb745-2d6e-4f7d-a6a1-bd42311ad82f",
                                                "0ab2b444-66cc-4041-b75d-503878b362eb")).build())
                                        .calculationField(VelocityConfigDTO.CalculationField.end_time)
                                        .isCdJob(true)
                                        .isCiJob(false)
                                        .filter(new HashMap<>())
                                        .build())
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType(application)
                                        .event(VelocityConfigDTO.Event.builder().values(Arrays.asList("e0a70eae-f092-466f-9d6a-fc950f843db5",
                                                "b70eb745-2d6e-4f7d-a6a1-bd42311ad82f",
                                                "0ab2b444-66cc-4041-b75d-503878b362eb",
                                                "0ab2b444-66cc-4f7d-a6a1-bd42311ad82f")).build())
                                        .calculationField(VelocityConfigDTO.CalculationField.start_time)
                                        .isCdJob(true)
                                        .isCiJob(false)
                                        .filter(new HashMap<>())
                                        .build())
                                .build())
                        .build())
                .build();

        CiCdJobRunsFilter expectedFDFilter = cicdJobRunFilter().toBuilder()
                .endTimeRange(ImmutablePair.nullPair())
                .build();
        CiCdJobRunsFilter expectedTDFilter = cicdJobRunFilter().toBuilder()
                .startTimeRange(ImmutablePair.nullPair())
                .build();

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(dummyRequest).build());
        when(ciCdDoraService.calculateNewChangeFailureRate(
                eq(company), eq(expectedFDFilter), eq(expectedTDFilter), any(), eq(velocityConfigDTO), any()
        )).thenReturn(DoraResponseDTO.builder().build());

        // execute
        doraService.generateCFRCountforCICD(company, dummyRequest, velocityConfigDTO);

        // assert
        verify(ciCdDoraService, times(1)).calculateNewChangeFailureRate(
                eq(company), eq(expectedFDFilter), eq(expectedTDFilter), any(), eq(velocityConfigDTO), any()
        );
    }

    @Test
    public void testStageStepFilterForDF() throws IOException, BadRequestException, SQLException {
        ArgumentCaptor<CiCdJobRunsFilter> deploymentFilterArgumentCaptor = ArgumentCaptor.forClass(CiCdJobRunsFilter.class);
        DefaultListRequest request = DefaultListRequest.builder()
                .filter(new HashMap<>(
                        Map.of(
                                "time_range", Map.of("$gt", 12345, "$lt", 78965),
                                "start_time", Map.of("$gt", 12345, "$lt", 78965),
                                "end_time", Map.of("$gt", 12345, "$lt", 78965)
                        )
                ))
                .ouIds(Set.of(13))
                .build();

        String resource = ResourceUtils.getResourceAsString("dora/velocity_with_stage_step_filters.json");
        VelocityConfigDTO velocityConfigDTO = objectMapper.readValue(resource, VelocityConfigDTO.class);

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(request).build());
        doraService.generateDFCountForCICD(company, request, velocityConfigDTO);
        then(ciCdDoraService).should().calculateNewDeploymentFrequency(eq(company), deploymentFilterArgumentCaptor.capture(), any(), any(), any());

        CiCdJobRunsFilter filter = deploymentFilterArgumentCaptor.getValue();
        Map<String, Object> expectedFilter = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getFilter();

        Assert.assertEquals(expectedFilter.get("stage_name"), filter.getStageNames());
        Assert.assertEquals(expectedFilter.get("step_name"), filter.getStepNames());
        Assert.assertEquals(((Map)expectedFilter.get("exclude")).get("stage_status"), filter.getExcludeStageStatuses());
        Assert.assertEquals(((Map)expectedFilter.get("exclude")).get("step_status"), filter.getExcludeStepStatuses());
    }

    @Test
    public void testStageStepFilterForCFR() throws IOException, SQLException, BadRequestException {
        ArgumentCaptor<CiCdJobRunsFilter> failedDeploymentFilterArgumentCaptor = ArgumentCaptor.forClass(CiCdJobRunsFilter.class);
        ArgumentCaptor<CiCdJobRunsFilter> totalDeploymentFilterArgumentCaptor = ArgumentCaptor.forClass(CiCdJobRunsFilter.class);
        DefaultListRequest request = DefaultListRequest.builder()
                .filter(new HashMap<>(
                        Map.of(
                                "time_range", Map.of("$gt", 12345, "$lt", 78965),
                                "start_time", Map.of("$gt", 12345, "$lt", 78965),
                                "end_time", Map.of("$gt", 12345, "$lt", 78965)
                        )
                ))
                .ouIds(Set.of(13))
                .build();

        String resource = ResourceUtils.getResourceAsString("dora/velocity_with_stage_step_filters.json");
        VelocityConfigDTO velocityConfigDTO = objectMapper.readValue(resource, VelocityConfigDTO.class);

        when(orgUnitHelper.getOuConfigurationFromRequest(anyString(), anySet(), any(DefaultListRequest.class)))
                .thenReturn(OUConfiguration.builder().request(request).build());

        doraService.generateCFRCountforCICD(company, request, velocityConfigDTO);

        verify(ciCdDoraService, atLeastOnce()).calculateNewChangeFailureRate(eq(company), failedDeploymentFilterArgumentCaptor.capture(), totalDeploymentFilterArgumentCaptor.capture(), any(), any(), any());

        CiCdJobRunsFilter failedDeploymentFilter = failedDeploymentFilterArgumentCaptor.getValue();
        CiCdJobRunsFilter totalDeploymentFilter = totalDeploymentFilterArgumentCaptor.getValue();
        Map<String, Object> expectedFailedDeploymentFilter = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getFilter();
        Map<String, Object> expectedTotalDeploymentFilter = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getFilter();

        Assert.assertEquals(((Map)expectedTotalDeploymentFilter.get("exclude")).get("stage_name"), totalDeploymentFilter.getExcludeStageNames());
        Assert.assertEquals(((Map) expectedFailedDeploymentFilter.get("exclude")).get("step_name"), failedDeploymentFilter.getExcludeStepNames());
    }


    private CiCdJobRunsFilter cicdJobRunFilter() {
        return CiCdJobRunsFilter.builder()
                .jobNormalizedFullNames(List.of())
                .endTimeRange(ImmutablePair.of(12345L, 78965L))
                .startTimeRange(ImmutablePair.of(12345L, 78965L))
                .across(CiCdJobRunsFilter.DISTINCT.trend)
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .aggInterval(CICD_AGG_INTERVAL.day)
                .stacks(List.of())
                .cicdUserIds(List.of())
                .jobNames(List.of())
                .jobStatuses(List.of())
                .instanceNames(List.of())
                .integrationIds(List.of())
                .isCiJob(false)
                .isCdJob(true)
                .triageRuleNames(List.of())
                .types(List.of())
                .parameters(List.of())
                .qualifiedJobNames(List.of())
                .partialMatch(Map.of())
                .projects(List.of())
                .orgProductsIds(Set.of())
                .sortBy(Map.of())
                .stageNames(List.of())
                .stepNames(List.of())
                .stageStatuses(List.of())
                .stepStatuses(List.of())
                .excludeStageNames(List.of())
                .excludeStepNames(List.of())
                .excludeStageStatuses(List.of())
                .excludeStepStatuses(List.of())
                .excludeJobNames(List.of())
                .excludeJobNormalizedFullNames(List.of())
                .excludeJobStatuses(List.of())
                .excludeInstanceNames(List.of())
                .excludeProjects(List.of())
                .excludeCiCdUserIds(List.of())
                .excludeTypes(List.of())
                .excludeQualifiedJobNames(List.of())
                .excludeTriageRuleNames(List.of())
                .services(List.of())
                .environments(List.of())
                .infrastructures(List.of())
                .repositories(List.of())
                .branches(List.of())
                .deploymentTypes(List.of())
                .tags(List.of())
                .excludeServices(List.of())
                .excludeEnvironments(List.of())
                .excludeInfrastructures(List.of())
                .excludeRepositories(List.of())
                .excludeBranches(List.of())
                .excludeDeploymentTypes(List.of())
                .excludeTags(List.of())
                .build();
    }
}
