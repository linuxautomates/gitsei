package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.triage.clients.TriageRESTClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class TriageRulesControllerTest {

    private MockMvc mvc;

    @Mock
    private TriageRESTClient triageRESTClient;

    private TriageRulesController triageRulesController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        triageRulesController = new TriageRulesController(triageRESTClient);
        mvc = MockMvcBuilders
                .standaloneSetup(triageRulesController)
                .build();
    }

    @Test
    public void testGetTriageRule() throws Exception {
        mvc.perform(put("/v1/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(TriageRule.builder().build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(put("/v1/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(TriageRule.builder().build())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testUpdateTriageRule() throws Exception {
        mvc.perform(get("/v1/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/v1/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testDeleteTriageRule() throws Exception {
        mvc.perform(delete("/v1/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(asyncDispatch(mvc.perform(delete("/v1/triage_rules/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testBilkDeleteTriageRules() throws Exception {
        mvc.perform(delete("/v1/triage_rules/")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--", "8d4b8020-7f52-49f3-9daf-15688206eaaa"))))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(asyncDispatch(mvc.perform(delete("/v1/triage_rules")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0", "8d4b8020-7f52-49f3-9daf-15688206eaaa"))))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
