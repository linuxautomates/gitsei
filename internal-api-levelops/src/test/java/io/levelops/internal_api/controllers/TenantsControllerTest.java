package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.databases.services.TenantService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class TenantsControllerTest {
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TenantService tenantService;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TenantsController(objectMapper,
                tenantService)).build();
    }

    @Test
    public void testCreateTenant() throws Exception {
        when(tenantService.insert(eq(null), eq(Tenant.builder()
                .tenantName("Hello").id("yello").build()))).thenReturn("yello");
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenant_name\":\"Hello\",\"id\":\"yello\"}"))
                .andReturn()))
                .andExpect(content().json("{\"tenant_id\":\"yello\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testListTenants() throws Exception {
        List<Tenant> l = Collections.singletonList(Tenant.builder()
                .tenantName("Hello").id("yello").build());
        DbListResponse<Tenant> expectedResponse = DbListResponse.of(l, 1);
        when(tenantService.list(null, 2, 1))
                .thenReturn(expectedResponse);
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page_size\":1,\"page\":2}"))
                .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }
}
