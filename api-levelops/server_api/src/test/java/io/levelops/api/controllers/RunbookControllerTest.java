package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.services.RunbookDTOService;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.RunbookDatabaseService;
import io.levelops.commons.databases.services.RunbookNodeTemplateDatabaseService;
import io.levelops.commons.databases.services.RunbookReportDatabaseService;
import io.levelops.commons.databases.services.RunbookRunDatabaseService;
import io.levelops.commons.databases.services.RunbookRunningNodeDatabaseService;
import io.levelops.commons.databases.services.RunbookTemplateCategoryDatabaseService;
import io.levelops.commons.databases.services.RunbookTemplateDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.runbooks.clients.RunbookClient;
import io.levelops.runbooks.services.RunbookReportService;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class RunbookControllerTest {

    private MockMvc mvc;

    private ObjectMapper objectMapper;

    @Mock
    private RunbookDatabaseService runbookDatabaseService;
    @Mock
    private RunbookNodeTemplateDatabaseService nodeTemplateDatabaseService;
    @Mock
    private RunbookRunDatabaseService runDatabaseService;
    @Mock
    private RunbookReportDatabaseService reportDatabaseService;
    @Mock
    private RunbookReportService reportService;
    @Mock
    private RunbookDTOService runbookDTOService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private RunbookRunningNodeDatabaseService runbookRunningNodeDatabaseService;
    @Mock
    private RunbookClient runbookClient;
    @Mock
    private RunbookTemplateDatabaseService templateDatabaseService;
    @Mock
    private RunbookTemplateCategoryDatabaseService templateCategoryDatabaseService;

    private RunbookController runbookController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        objectMapper = DefaultObjectMapper.get();
        runbookController = new RunbookController(objectMapper, runbookDatabaseService, nodeTemplateDatabaseService,
                runDatabaseService, reportDatabaseService, reportService, runbookDTOService, activityLogService,
                runbookRunningNodeDatabaseService, runbookClient, templateDatabaseService, templateCategoryDatabaseService);
        mvc = MockMvcBuilders
                .standaloneSetup(runbookController)
                .build();
    }

    @Test
    public void testGetByPermanentId() throws Exception {
        mvc.perform(get("/v1/playbooks/permanent-id/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/v1/playbooks/permanent-id/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testBulkDeleteRunbooks() throws Exception {
        mvc.perform(delete("/v1/playbooks")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--"))))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(delete("/v1/playbooks")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0"))))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testDeleteRunbookByPermanentId() throws Exception {
        mvc.perform(delete("/v1/playbooks/permanent-id/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(delete("/v1/playbooks/permanent-id/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testUpdateRunbook() throws Exception {
        mvc.perform(put("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(Runbook.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(put("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(Runbook.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").build())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testUpdateRunbookByPermanentId() throws Exception {
        mvc.perform(put("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(Runbook.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").permanentId("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(put("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(Runbook.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").permanentId("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").build())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testBulkDeleteRuns() throws Exception {
        mvc.perform(delete("/v1/playbooks/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--"))))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(delete("/v1/playbooks/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0"))))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testBulkDeleteReports() throws Exception {
        mvc.perform(delete("/v1/playbooks/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--"))))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(delete("/v1/playbooks/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0"))))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testGetReport() throws Exception {
        mvc.perform(get("/v1/playbooks/reports/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/v1/playbooks/reports/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testListRunNodes() throws Exception {
        mvc.perform(post("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0/runs/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--/nodes/list")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(DefaultListRequest.builder().build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(post("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0/runs/f73dbe99-5318-4d0c-abaa-fe3c397e63e0/nodes/list")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(DefaultListRequest.builder().build())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testTriggerPlaybookManually() throws Exception {
        mvc.perform(post("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--/trigger/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(Map.of())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(post("/v1/playbooks/f73dbe99-5318-4d0c-abaa-fe3c397e63e0/trigger/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(Map.of())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
