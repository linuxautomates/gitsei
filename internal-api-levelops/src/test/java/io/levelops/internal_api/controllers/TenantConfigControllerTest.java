package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class TenantConfigControllerTest {
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TenantConfigService tenantConfigService;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TenantConfigController(objectMapper,
                tenantConfigService)).build();
    }

    @Test
    public void testCreateTenant() throws Exception {
        when(tenantConfigService.listByFilter(any(), any(), any(), any())).thenReturn(
                DbListResponse.of(List.of(
                        TenantConfig.builder()
                                .id("test-id")
                                .name("test-name")
                                .value("test-value")
                                .build()
                ), 1));
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenant_config/test")
                                .queryParam("config_key", "test-id"))
                        .andReturn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_count").value(1))
                .andExpect(jsonPath("$.records").exists());
    }
}