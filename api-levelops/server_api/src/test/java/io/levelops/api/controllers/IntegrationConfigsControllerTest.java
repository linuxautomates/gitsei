package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationConfig.Metadata;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class IntegrationConfigsControllerTest {

    private static final String COMPANY = "test";
    private MockMvc mvc;

    @Mock
    private IntegrationService integrationService;

    private IntegrationConfigsController integrationConfigsController;

    private static String configStr = "{\n" +
            "  \"integration_id\": \"5\",\n" +
            "  \"custom_hygienes\": [\n" +
            "    {\n" +
            "      \"id\": \"3505ad50-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Story Points\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10014\": true\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"43e533e0-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Acceptance Criteria\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10070\": true\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"config\": {\n" +
            "    \"epic_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10008\",\n" +
            "        \"name\": \"Epic Link\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"agg_custom_fields\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"Story Points\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"key\": \"customfield_10302\",\n" +
            "        \"name\": \"Category\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"salesforce_fields\": [],\n" +
            "    \"story_points_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"story points field\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private static String existingConfigStr = "{\n" +
            "  \"integration_id\": \"5\",\n" +
            "  \"custom_hygienes\": [\n" +
            "    {\n" +
            "      \"id\": \"3505ad50-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Story Points\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10014\": true\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"43e533e0-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Acceptance Criteria\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10070\": true\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"config\": {\n" +
            "    \"epic_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10008\",\n" +
            "        \"name\": \"Epic Link\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"agg_custom_fields\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"Story Points\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"salesforce_fields\": [],\n" +
            "    \"story_points_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"story points field\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"config_updated_at\": 1689907043\n" +
            "  }\n" +
            "}";

    private static String existingConfigStr1 = "{\n" +
            "  \"integration_id\": \"5\",\n" +
            "  \"custom_hygienes\": [\n" +
            "    {\n" +
            "      \"id\": \"3505ad50-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Story Points\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10014\": true\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"43e533e0-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Acceptance Criteria\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10070\": true\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"config\": {\n" +
            "    \"epic_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10008\",\n" +
            "        \"name\": \"Epic Link\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"agg_custom_fields\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"Story Points\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"salesforce_fields\": [],\n" +
            "    \"story_points_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"story points field\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"random_field\": 1689907043\n" +
            "  }\n" +
            "}";



    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        integrationConfigsController = new IntegrationConfigsController(integrationService);
        mvc = MockMvcBuilders
                .standaloneSetup(integrationConfigsController)
                .build();
    }

    @Test
    public void insertCustomFieldsNoExistingConfig() throws Exception {
        // -- scenario 1, no existing config
        when(integrationService.listConfigs(eq(COMPANY), eq(List.of("1")), any(), any()))
                .thenReturn(DbListResponse.<IntegrationConfig>builder().build());

        IntegrationConfig payload = IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of())
                .build();
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/integration_configs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .sessionAttr("company", COMPANY)
                                .sessionAttr("session_user", "test")
                                .content(DefaultObjectMapper.get().writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        ArgumentCaptor<IntegrationConfig> integrationConfigArgumentCaptor = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(integrationService, times(1)).insertConfig(eq(COMPANY), integrationConfigArgumentCaptor.capture());
        assertThat(integrationConfigArgumentCaptor.getValue().getMetadata().getConfigUpdatedAt()).isNotNull();
    }

    @Test
    public void insertCustomFieldsExistingConfigNoChange() throws Exception {
        // -- scenario 1, no existing config
        when(integrationService.listConfigs(eq(COMPANY), eq(List.of("1")), any(), any()))
                .thenReturn(DbListResponse.<IntegrationConfig>builder().record(IntegrationConfig.builder()
                        .integrationId("1")
                        .config(Map.of("agg_custom_fields",
                                List.of(
                                        IntegrationConfig.ConfigEntry.builder().key("1").name("A").build(),
                                        IntegrationConfig.ConfigEntry.builder().key("2").name("B").build())))
                        .metadata(Metadata.builder().configUpdatedAt(1234L).build())
                        .build()).build());

        IntegrationConfig payload = IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of("agg_custom_fields",
                        List.of(
                                IntegrationConfig.ConfigEntry.builder().key("1").name("A").build(),
                                IntegrationConfig.ConfigEntry.builder().key("2").name("B").build())))
                .build();
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/integration_configs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .sessionAttr("company", COMPANY)
                                .sessionAttr("session_user", "test")
                                .content(DefaultObjectMapper.get().writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        ArgumentCaptor<IntegrationConfig> integrationConfigArgumentCaptor = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(integrationService, times(1)).insertConfig(eq(COMPANY), integrationConfigArgumentCaptor.capture());
        assertThat(integrationConfigArgumentCaptor.getValue().getMetadata().getConfigUpdatedAt()).isEqualTo(1234L);
    }

    @Test
    public void insertCustomFieldsExistingConfigChanged() throws Exception {
        // -- scenario 1, no existing config
        when(integrationService.listConfigs(eq(COMPANY), eq(List.of("1")), any(), any()))
                .thenReturn(DbListResponse.<IntegrationConfig>builder().record(IntegrationConfig.builder()
                        .integrationId("1")
                        .config(Map.of("agg_custom_fields",
                                List.of(
                                        IntegrationConfig.ConfigEntry.builder().key("1").name("A").build(),
                                        IntegrationConfig.ConfigEntry.builder().key("2").name("B").build())))
                        .metadata(Metadata.builder()
                                .configUpdatedAt(1234L)
                                .build())
                        .build()).build());

        IntegrationConfig payload = IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of("agg_custom_fields",
                        List.of(
                                IntegrationConfig.ConfigEntry.builder().key("3").name("A").build(), // ---- CHANGED
                                IntegrationConfig.ConfigEntry.builder().key("2").name("B").build())))
                .metadata(Metadata.builder().build())
                .build();
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/integration_configs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .sessionAttr("company", COMPANY)
                                .sessionAttr("session_user", "test")
                                .content(DefaultObjectMapper.get().writeValueAsString(payload)))
                        .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        ArgumentCaptor<IntegrationConfig> integrationConfigArgumentCaptor = ArgumentCaptor.forClass(IntegrationConfig.class);
        verify(integrationService, times(1)).insertConfig(eq(COMPANY), integrationConfigArgumentCaptor.capture());
        assertThat(integrationConfigArgumentCaptor.getValue().getMetadata().getConfigUpdatedAt()).isNotNull();
        assertThat(integrationConfigArgumentCaptor.getValue().getMetadata().getConfigUpdatedAt()).isNotEqualTo(1234L);
    }

    @Test
    public void testGetNewConfigUpdatedAt() throws JsonProcessingException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        IntegrationConfigsController integrationConfigsController = new IntegrationConfigsController(null);

        IntegrationConfig config = mapper.readValue(configStr, IntegrationConfig.class);
        IntegrationConfig existingConfig = mapper.readValue(existingConfigStr, IntegrationConfig.class);

        long now = Instant.now().getEpochSecond();

        Long customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig, config);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(now);

        IntegrationConfig existingConfig1 = mapper.readValue(existingConfigStr, IntegrationConfig.class);

        customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig, existingConfig1);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(1689907043);

        customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, config, existingConfig);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(now);

        IntegrationConfig existingConfig2 = mapper.readValue(existingConfigStr1, IntegrationConfig.class);
        customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig2, config);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(now);

        existingConfig2 = existingConfig2.toBuilder()
                .metadata(null)
                .build();
        customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig2, config);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(now);
    }
}