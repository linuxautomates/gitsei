package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.services.IntegrationSecretsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class IntegrationsControllerTest {
    private MockMvc mvc;

    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private TagItemDBService tagItemDBService;
    @Autowired
    private ProductIntegMappingService productIntegMappingService;
    @Autowired
    private IntegrationSecretsService integrationSecretsService;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new IntegrationsController(integrationService, tagItemDBService, productIntegMappingService, integrationSecretsService)).build();
    }

    @Test
    public void testCreateIntegration() throws Exception {
        Integration integration = Integration.builder()
                .url("asd")
                .description("askdj")
                .name("asjkdj")
                .status("ACTIVE")
                .tags(List.of("1", "2"))
                .application("github")
                .satellite(true)
                .build();
        when(integrationService.insert("test", integration)).thenReturn("1");

        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/integrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DefaultObjectMapper.get().writeValueAsString(integration)))
                .andReturn()))
                .andExpect(content().json("{\"integration_id\":\"1\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testGetIntegration() throws Exception {
        when(integrationService.get("test", "1"))
                .thenReturn(Optional.of(Integration.builder().url("asd").description("askdj")
                        .name("asjkdj").status("ACTIVE").application("github").build()));
        when(integrationService.get("test", "2"))
                .thenReturn(Optional.empty());
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/integrations/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()))
                .andExpect(status().isOk());
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/integrations/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()))
                .andExpect(status().isNotFound());
    }
}
