package io.levelops.api.controllers;

import io.levelops.api.services.WorkItemService;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItem.TicketType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.workitems.clients.WorkItemsClient;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class WorkItemsControllerTest {
    private MockMvc mvc;

    @Mock
    private WorkItemsClient workItemsClient;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private UserService userService;
    @Mock
    private WorkItemService workItemService;

    private WorkItemsController workItemsController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        workItemsController = new WorkItemsController(userService, workItemsClient, activityLogService, workItemService);
        mvc = MockMvcBuilders
                .standaloneSetup(workItemsController)
                .build();
    }
    
    @Test
    public void testCreateWorkItem() throws Exception {
        WorkItem workItem = WorkItem.builder()
                .priority(Severity.UNKNOWN)
                .type(WorkItem.ItemType.MANUAL)
                .ticketType(TicketType.WORK_ITEM)
                .cloudOwner("cloudOwner")
                .artifactTitle("artifactTitle")
                .dueAt(Instant.now().getEpochSecond())
                .reason("because")
                .productId("1")
                .artifact("artifact")
                .assignee(WorkItem.Assignee.builder().userId("1").build())
                .assignee(WorkItem.Assignee.builder().userId("2").build())
                .ticketTemplateId(UUID.randomUUID().toString())
                .tagId("1")
                .tagId("2")
                .stateId(1)
                .attachment(WorkItem.Attachment.builder().uploadId(UUID.randomUUID().toString()).comment("upload 1").build())
                .title("Ticket Title")
                .reporter("abc@test.com")
                .build();

        WorkItem createdWI = workItem.toBuilder().id("1").build();

        doReturn(createdWI).when(workItemsClient).create(eq("test"), any());
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/workitems")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(workItem)))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        HashMap<String, Long> range = new HashMap<>();
        range.put("$lt", 1990451199L);
        range.put("$gt", 1190364800L);
        mvc.perform(post("/v1/workitems/list")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .sessionAttr("user_type", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(DefaultListRequest.builder().filter(Map.of("created_at", range)).build())))
                .andExpect(status().is(HttpStatus.OK.value()));

        mvc.perform(post("/v1/workitems")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(workItem.toBuilder().ticketTemplateId(null).build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
        
        mvc.perform(post("/v1/workitems")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(workItem.toBuilder().ticketTemplateId("1").build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(workItemsClient, times(1)).create(anyString(), any());
    }
}