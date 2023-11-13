package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.services.DoraService;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraDrillDownDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.service.dora.LegacyLeadTimeCalculationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class DoraControllerTest {
    @Autowired
    private DoraService doraService;

    @Autowired
    private LegacyLeadTimeCalculationService legacyLeadTimeCalculation;

    private String longerCacheExpiryTenantsString = "test,foo";

    private MockMvc mvc;
    private DoraController doraController;
    private Integer integrationId = 12;

    @Before
    public void setup() {
        doraController = new DoraController(doraService, legacyLeadTimeCalculation, longerCacheExpiryTenantsString);
        mvc = MockMvcBuilders.standaloneSetup(doraController).build();
    }

    // add CICD/IM related UT for CFR & DF
    @Test
    public void testCalculateDeploymentFrequency() throws Exception {

        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder()
                        .integrationId(integrationId)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .deploymentFrequency(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType("SCM")
                                        .build())
                                .build())
                        .build())
                .build();
        when(doraService.getVelocityConfigByOu(anyString(), any())).thenReturn(velocityConfigDTOForSCM);
        when(doraService.generateDeploymentFrequencyForSCM(anyString(), any(DefaultListRequest.class),
                anyBoolean(), any(VelocityConfigDTO.class)))
                .thenReturn(DoraResponseDTO.builder().build());

        mvc.perform(post("/v1/dora/deployment_frequency")
                        .sessionAttr("company", "test")
                        .requestAttr("there_is_no_cache", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "    \"filter\": {\n" +
                                "        \"metric\": \"resolve\",\n" +
                                "        \"velocity_config_id\": \"4156e91b-bae9-4d7b-8a63-178f0c60217f\",\n" +
                                "        \"pr_merged_at\": {\n" +
                                "            \"$gt\":\"1668470400\",\n" +
                                "            \"$lt\":\"1668556799\"\n" +
                                "        },\n" +
                                "        \"integration_ids\": [\n" +
                                "            \"1849\"\n" +
                                "        ]\n" +
                                "    },\n" +
                                "    \"ou_ids\": [\n" +
                                "        \"18092\",\n" +
                                "        \"14924\"\n" +
                                "    ],\n" +
                                "    \"widget_id\": \"ca4e8100-63e0-11ed-9193-27d8ad921761\"\n" +
                                "}"))
                .andExpect(status().is(200))
                .andReturn();

    }

    @Test
    public void testCalculateChangeFailureRate() throws Exception {

        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(integrationId)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .totalDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType("SCM")
                                        .build())
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .integrationType("SCM")
                                        .build())
                                .build())
                        .build())
                .build();
        when(doraService.getVelocityConfigByOu(anyString(), any())).thenReturn(velocityConfigDTOForSCM);
        when(doraService.generateChangeFailureRateForSCM(anyString(), any(),
                anyBoolean(), any()))
                .thenReturn(DoraResponseDTO.builder()
                        .stats(DoraSingleStateDTO.builder()
                                .failureRate(25.5)
                                .build())
                        .build());

        mvc.perform(post("/v1/dora/change_failure_rate")
                        .sessionAttr("company", "test")
                        .requestAttr("there_is_no_cache", false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "    \"filter\": {\n" +
                                "        \"metric\": \"resolve\",\n" +
                                "        \"velocity_config_id\": \"4156e91b-bae9-4d7b-8a63-178f0c60217f\",\n" +
                                "        \"pr_merged_at\": {\n" +
                                "            \"$gt\":\"1668470400\",\n" +
                                "            \"$lt\":\"1668556799\"\n" +
                                "        },\n" +
                                "        \"integration_ids\": [\n" +
                                "            \"1849\"\n" +
                                "        ]\n" +
                                "    },\n" +
                                "    \"ou_ids\": [\n" +
                                "        \"18092\",\n" +
                                "        \"14924\"\n" +
                                "    ],\n" +
                                "    \"widget_id\": \"ca4e8100-63e0-11ed-9193-27d8ad921761\"\n" +
                                "}"))
                .andExpect(status().is(200))
                .andReturn();
    }


    @Test
    public void testScmCommitsDrillDownForDF() throws Exception {
        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .associatedOURefIds(List.of("32110"))
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder().build())
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
        when(doraService.getVelocityConfigByOu(anyString(), any())).thenReturn(velocityConfigDTOForSCM);
        when(doraService.getScmCommitList(anyBoolean(), anyString(), any(DefaultListRequest.class),
                any(VelocityConfigDTO.class)))
                .thenReturn(DbListResponse.<DoraDrillDownDTO>builder().build());

        mvc.perform(
                        post("/v1/dora/drilldown/scm-commits/list")
                                .sessionAttr("company", "test")
                                .requestAttr("there_is_no_cache", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"page\": 0,\n" +
                                        "    \"page_size\": 10,\n" +
                                        "    \"filter\": {\n" +
                                        "        \"time_range\": {\n" +
                                        "            \"$lt\": \"1666828800\",\n" +
                                        "            \"$gt\": \"1666569600\"\n" +
                                        "        },\n" +
                                        "        \"across\": \"velocity\"\n" +
                                        "    },\n" +
                                        "    \"ou_ids\": [\n" +
                                        "        \"32110\"\n" +
                                        "    ],\n" +
                                        "    \"widget\": \"deployment_frequency\",\n" +
                                        "    \"widget_id\": \"e3a13490-967b-11ed-b20d-ed5e0e447b12\"\n" +
                                        "}")
                )
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testScmCommitsDrillDownForCFR() throws Exception {
        VelocityConfigDTO velocityConfigDTOForSCM = VelocityConfigDTO.builder()
                .associatedOURefIds(List.of("32110"))
                .changeFailureRate(VelocityConfigDTO.ChangeFailureRate.builder()
                        .integrationId(1)
                        .velocityConfigFilters(VelocityConfigDTO.VelocityConfigFilters.builder()
                                .failedDeployment(VelocityConfigDTO.FilterTypes.builder()
                                        .scmFilters(new HashMap<>(Map.of("commit_branch", Map.of("$begins", List.of("hotfix")))))
                                        .integrationType("SCM")
                                        .build())
                                .build())
                        .build())
                .deploymentFrequency(VelocityConfigDTO.DeploymentFrequency.builder().build())
                .build();
        when(doraService.getVelocityConfigByOu(anyString(), any())).thenReturn(velocityConfigDTOForSCM);
        when(doraService.getScmCommitList(anyBoolean(), anyString(), any(DefaultListRequest.class),
                any(VelocityConfigDTO.class)))
                .thenReturn(DbListResponse.<DoraDrillDownDTO>builder().build());

        mvc.perform(
                        post("/v1/dora/drilldown/scm-commits/list")
                                .sessionAttr("company", "test")
                                .requestAttr("there_is_no_cache", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"page\": 0,\n" +
                                        "    \"page_size\": 10,\n" +
                                        "    \"filter\": {\n" +
                                        "        \"time_range\": {\n" +
                                        "            \"$lt\": \"1666828800\",\n" +
                                        "            \"$gt\": \"1666569600\"\n" +
                                        "        },\n" +
                                        "        \"across\": \"velocity\"\n" +
                                        "    },\n" +
                                        "    \"ou_ids\": [\n" +
                                        "        \"32110\"\n" +
                                        "    ],\n" +
                                        "    \"widget\": \"change_failure_rate\",\n" +
                                        "    \"widget_id\": \"e3a13490-967b-11ed-b20d-ed5e0e447b12\"\n" +
                                        "}")
                )
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testGetVelocityAggsForLeadTime() throws Exception {
        // configure
        when(legacyLeadTimeCalculation.getNewVelocityAggsForLeadTime(eq("test"), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(Collections.singletonList(DbAggregationResult.builder().build()));

        // execute
        mvc.perform(
                        post("/v1/dora/lead-time")
                                .sessionAttr("company", "test")
                                .requestAttr("there_is_no_cache", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"filter\": {\n" +
                                        "        \"ratings\": [\n" +
                                        "            \"good\",\n" +
                                        "            \"slow\",\n" +
                                        "            \"needs_attention\"\n" +
                                        "        ],\n" +
                                        "        \"limit_to_only_applicable_data\": true,\n" +
                                        "        \"integration_ids\": [\n" +
                                        "            \"1849\",\n" +
                                        "            \"4002\",\n" +
                                        "            \"4003\"\n" +
                                        "        ]\n" +
                                        "    },\n" +
                                        "    \"across\": \"velocity\",\n" +
                                        "    \"ou_ids\": [\n" +
                                        "        \"32110\"\n" +
                                        "    ],\n" +
                                        "    \"ou_user_filter_designation\": {\n" +
                                        "        \"jira\": [\n" +
                                        "            \"none\"\n" +
                                        "        ]\n" +
                                        "    },\n" +
                                        "    \"widget_id\": \"a05d6fa0-b66a-11ed-943e-d524018a0ff4\"\n" +
                                        "}")
                )
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testGetVelocityAggsForMeanTime() throws Exception {
        // configure
        when(legacyLeadTimeCalculation.getNewVelocityAggsForMeanTime(eq("test"), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(Collections.singletonList(DbAggregationResult.builder().build()));

        // execute
        mvc.perform(
                        post("/v1/dora/mean-time")
                                .sessionAttr("company", "test")
                                .requestAttr("there_is_no_cache", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"filter\": {\n" +
                                        "        \"ratings\": [\n" +
                                        "            \"good\",\n" +
                                        "            \"slow\",\n" +
                                        "            \"needs_attention\"\n" +
                                        "        ],\n" +
                                        "        \"limit_to_only_applicable_data\": true,\n" +
                                        "        \"integration_ids\": [\n" +
                                        "            \"1849\",\n" +
                                        "            \"4002\",\n" +
                                        "            \"4003\"\n" +
                                        "        ]\n" +
                                        "    },\n" +
                                        "    \"across\": \"velocity\",\n" +
                                        "    \"ou_ids\": [\n" +
                                        "        \"32110\"\n" +
                                        "    ],\n" +
                                        "    \"ou_user_filter_designation\": {\n" +
                                        "        \"jira\": [\n" +
                                        "            \"none\"\n" +
                                        "        ]\n" +
                                        "    },\n" +
                                        "    \"widget_id\": \"a05d6fa0-b66a-11ed-943e-d524018a0ff4\"\n" +
                                        "}")
                )
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testGetVelocityValuesForLeadTime() throws Exception {
        // configure
        when(legacyLeadTimeCalculation.getVelocityValuesForLeadTime(eq("test"), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(DbListResponse.<DbAggregationResult>builder().build());

        // execute
        mvc.perform(
                        post("/v1/dora/lead-time/drilldown")
                                .sessionAttr("company", "test")
                                .requestAttr("there_is_no_cache", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"page\": 0,\n" +
                                        "    \"page_size\": 10,\n" +
                                        "    \"filter\": {\n" +
                                        "        \"ratings\": [\n" +
                                        "            \"good\",\n" +
                                        "            \"slow\",\n" +
                                        "            \"needs_attention\"\n" +
                                        "        ],\n" +
                                        "        \"limit_to_only_applicable_data\": false,\n" +
                                        "        \"integration_ids\": [\n" +
                                        "            \"1849\",\n" +
                                        "            \"4002\",\n" +
                                        "            \"4003\"\n" +
                                        "        ],\n" +
                                        "        \"histogram_stage_name\": \"Merge Time\"\n" +
                                        "    },\n" +
                                        "    \"across\": \"values\",\n" +
                                        "    \"ou_ids\": [\n" +
                                        "        \"32868\"\n" +
                                        "    ],\n" +
                                        "    \"ou_user_filter_designation\": {\n" +
                                        "        \"jira\": [\n" +
                                        "            \"none\"\n" +
                                        "        ]\n" +
                                        "    },\n" +
                                        "    \"ou_exclusions\": [\n" +
                                        "        \"values\"\n" +
                                        "    ],\n" +
                                        "    \"widget_id\": \"a05d6fa0-b66a-11ed-943e-d524018a0ff4\"\n" +
                                        "}")
                )
                .andExpect(status().is(200))
                .andReturn();
    }


    @Test
    public void testGetVelocityValuesForMeanTime() throws Exception {
        // configure
        when(legacyLeadTimeCalculation.getVelocityValuesForMeanTime(eq("test"), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(DbListResponse.<DbAggregationResult>builder().build());

        // execute
        mvc.perform(
                        post("/v1/dora/mean-time/drilldown")
                                .sessionAttr("company", "test")
                                .requestAttr("there_is_no_cache", false)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"page\": 0,\n" +
                                        "    \"page_size\": 10,\n" +
                                        "    \"filter\": {\n" +
                                        "        \"ratings\": [\n" +
                                        "            \"good\",\n" +
                                        "            \"slow\",\n" +
                                        "            \"needs_attention\"\n" +
                                        "        ],\n" +
                                        "        \"limit_to_only_applicable_data\": false,\n" +
                                        "        \"integration_ids\": [\n" +
                                        "            \"1849\",\n" +
                                        "            \"4002\",\n" +
                                        "            \"4003\"\n" +
                                        "        ],\n" +
                                        "        \"histogram_stage_name\": \"Merge Time\"\n" +
                                        "    },\n" +
                                        "    \"across\": \"values\",\n" +
                                        "    \"ou_ids\": [\n" +
                                        "        \"32868\"\n" +
                                        "    ],\n" +
                                        "    \"ou_user_filter_designation\": {\n" +
                                        "        \"jira\": [\n" +
                                        "            \"none\"\n" +
                                        "        ]\n" +
                                        "    },\n" +
                                        "    \"ou_exclusions\": [\n" +
                                        "        \"values\"\n" +
                                        "    ],\n" +
                                        "    \"widget_id\": \"a05d6fa0-b66a-11ed-943e-d524018a0ff4\"\n" +
                                        "}")
                )
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testGetCicdJobParams() throws Exception {
        // configure
        when(doraService.getCicdJobParams("test", List.of("cicd-job-id1", "cicd-job-id2", "cicd-job-id3")))
                .thenReturn(Map.of(
                        "nameA", List.of("valueA1", "valueA2"),
                        "nameB", List.of("valueB1", "valueB2", "valueB3"),
                        "nameC", List.of("valueC1")
                ));

        // execute
        mvc.perform(
                        post("/v1/dora/cicd-job-params")
                                .sessionAttr("company", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "    \"filter\": {\n" +
                                        "        \"cicd_job_ids\": [\n" +
                                        "            \"cicd-job-id1\",\n" +
                                        "            \"cicd-job-id2\",\n" +
                                        "            \"cicd-job-id3\"\n" +
                                        "        ]\n" +
                                        "    }\n" +
                                        "}\n")
                ).andExpect(status().is(200))
                .andReturn();
    }
}
