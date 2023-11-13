package io.levelops.internal_api.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItem.TicketType;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.events.clients.EventsClient;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.services.WorkItemService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class WorkItemsControllerTest {
    private MockMvc mvc;

    @Mock
    private WorkItemService workItemService;
    @Mock
    private EventsClient eventsClient;
    @Mock
    Storage storage;

    private WorkItemsController workItemsController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        workItemsController = new WorkItemsController(workItemService, storage, DefaultObjectMapper.get(), eventsClient);
        mvc = MockMvcBuilders
                .standaloneSetup(workItemsController)
                .build();
    }

    @Test
    public void testCreateWorkItem() throws Exception {
        ArgumentCaptor<WorkItem> workItemCaptor = ArgumentCaptor.forClass(WorkItem.class);

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

        doReturn(createdWI).when(workItemService).createWorkItem(eq("test"), workItemCaptor.capture());
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/workitems")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DefaultObjectMapper.get().writeValueAsString(workItem)))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
        
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/workitems")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(workItem.toBuilder().ticketTemplateId(null).build())))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
        
        mvc.perform(post("/internal/v1/tenants/test/workitems")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(workItem.toBuilder().ticketTemplateId("1").build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        verify(workItemService, times(2)).createWorkItem(anyString(), any());
        Assert.assertEquals(2, workItemCaptor.getAllValues().size());
        WorkItem a = workItemCaptor.getAllValues().get(0);
        Assert.assertEquals(workItem, a);
    }

    @Test
    public void testGetWorkItem() throws Exception {
        WorkItem workItem = WorkItem.builder()
                .priority(Severity.UNKNOWN)
                .type(WorkItem.ItemType.MANUAL)
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
                .vanityId("DEFAULT-1026")
                .build();

        doReturn(Optional.of(workItem)).when(workItemService).getWorkItem(eq("test"), eq(UUID.fromString("8b07e207-0401-4006-a67f-c84d86603817")));
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/workitems/8b07e207-0401-4006-a67f-c84d86603817"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testGetWorkItemByVanityId() throws Exception {
        WorkItem workItem = WorkItem.builder()
                .priority(Severity.UNKNOWN)
                .type(WorkItem.ItemType.MANUAL)
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
                .vanityId("DEFAULT-1026")
                .build();
        doReturn(Optional.of(workItem)).when(workItemService).getWorkItemByVanityId(eq("test"), eq("DEFAULT-1026"));
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/workitems/vanity-id/DEFAULT-1026"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testGetWorkItemValidation() throws Exception {
        WorkItem workItem = WorkItem.builder()
                .priority(Severity.UNKNOWN)
                .type(WorkItem.ItemType.MANUAL)
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
                .vanityId("DEFAULT-1026")
                .build();

        doReturn(Optional.of(workItem)).when(workItemService).getWorkItem(eq("test"), eq(UUID.fromString("8b07e207-0401-4006-a67f-c84d86603817")));
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/workitems/8b07e207-0401-4006-a67f-c84d86603817"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        mvc.perform(
                get("/internal/v1/tenants/test/workitems/8b07e207-0401-4006-a67f-c84d86603817'OR id not null'"))
                .andExpect(status().isNotFound());

    }

    @Test
    public void testUpdateWorkItemValidation() throws Exception {
        when(workItemService.deleteWorkItem(eq("test"),eq(""), eq(RoleType.ADMIN), eq(UUID.fromString("8b07e207-0401-4006-a67f-c84d86603817")))).thenReturn(true);
        mvc.perform(asyncDispatch(mvc.perform(delete("/internal/v1/tenants/test/workitems/8b07e207-0401-4006-a67f-c84d86603817"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        mvc.perform(
                delete("/internal/v1/tenants/test/workitems/8b07e207-0401-4006-a67f-c84d86603817'OR id not null'"))
                .andExpect(status().isNotFound());

    }
}