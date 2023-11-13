package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        IntegrationsController.class,
        DefaultApiTestConfiguration.class
})
public class TriageRulesControllerTest {

    private ObjectMapper objectMapper;
    @Mock
    private TriageRulesService triageRulesService;
    private MockMvc mvc;

    @Before
    public void setUp() throws Exception {
        objectMapper = DefaultObjectMapper.get();
        Mockito.reset(triageRulesService);
        TriageRulesController triageRulesController = new TriageRulesController(triageRulesService);
        mvc = MockMvcBuilders.standaloneSetup(triageRulesController).build();
    }

    @Test
    public void testGetRule() throws Exception {
        mvc.perform(get("/internal/v1/tenants/foo/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/internal/v1/tenants/foo/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testUpdateRule() throws Exception {
        mvc.perform(put("/internal/v1/tenants/foo/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo")
                .content(objectMapper.writeValueAsString(TriageRule.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(put("/internal/v1/tenants/foo/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo")
                .content(objectMapper.writeValueAsString(TriageRule.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").build())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testDeleteRule() throws Exception {
        mvc.perform(delete("/internal/v1/tenants/foo/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(delete("/internal/v1/tenants/foo/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testBulkDeleteRule() throws Exception {
        mvc.perform(delete("/internal/v1/tenants/foo/triage_rules")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo")
                .content(objectMapper.writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--"))))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(delete("/internal/v1/tenants/foo/triage_rules")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "foo")
                .sessionAttr("session_user", "foo")
                .content(objectMapper.writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0"))))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
