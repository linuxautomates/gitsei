package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.Questionnaire.State;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.questionnaire.GcsQuestionnaireDTO;
import io.levelops.commons.databases.models.response.QuestionnaireDetails;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.databases.services.QuestionnaireDBService;
import io.levelops.commons.databases.services.QuestionnaireTemplateDBService;
import io.levelops.commons.databases.services.SectionsService;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.databases.services.WorkItemDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.services.LocalMentionsService;
import io.levelops.internal_api.services.MentionsService;
import io.levelops.internal_api.services.QuestionnaireService;
import io.levelops.internal_api.services.QuestionnaireTemplateService;
import io.levelops.internal_api.services.QuestionnairesNotificationService;
import io.levelops.internal_api.services.TagItemService;
import io.levelops.notification.services.NotificationService;
import io.levelops.services.TemplateService;
import io.levelops.uploads.services.FilesService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        IntegrationsController.class,
        DefaultApiTestConfiguration.class
})
public class QuestionnairesControllerTest {
    private final String bucketName = "bucketName";
    private final String appBaseUrl = "http://test.test.tes/";
    private MockMvc mvc;
    @Mock
    private TagItemService tagItemService;
    @Mock
    private QuestionnaireTemplateDBService qTemplateDBService;
    private QuestionnaireTemplateService qTemplateService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private QuestionnaireDBService questionnaireService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private Storage storage;
    @Mock
    private SectionsService sectionsService;
    @Mock
    private WorkItemDBService workItemService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MsgTemplateService msgTemplateService;
    @Mock
    private StateDBService stateDBService;
    @Mock
    private EventsClient eventsClient;
    @Mock
    private TicketTemplateDBService ticketTemplateDBService;
    @Mock
    private FilesService filesService;
    @Mock
    private QuestionnairesNotificationService questionnairesNotificationService;
    @Mock
    private TemplateService templateService;

    private MentionsService mentionsService;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        qTemplateService = new QuestionnaireTemplateService(tagItemService, qTemplateDBService);
        QuestionnaireService srv = new QuestionnaireService(
                qTemplateService,
                msgTemplateService,
                integrationService,
                notificationService,
                sectionsService,
                questionnaireService,
                workItemService,
                bucketName,
                appBaseUrl,
                storage,
                objectMapper,
                stateDBService,
                ticketTemplateDBService,
                filesService,
                mentionsService, questionnairesNotificationService, templateService);
        mvc = MockMvcBuilders
                .standaloneSetup(new QuestionnairesController(srv, eventsClient, DefaultObjectMapper.get()))
                .build();
        mentionsService = new LocalMentionsService(notificationService);
    }

    @Test
    public void testCreateQuestionnaire() throws Exception {
        QuestionnaireTemplateService qTemplateService = new QuestionnaireTemplateService(tagItemService, qTemplateDBService);
        SectionsService sectionsService = Mockito.mock(SectionsService.class);
        QuestionnaireDBService questionnaireService = Mockito.mock(QuestionnaireDBService.class);
        MsgTemplateService msgTemplateService = Mockito.mock(MsgTemplateService.class);
        QuestionnaireService srv = new QuestionnaireService(
                qTemplateService,
                msgTemplateService,
                integrationService,
                notificationService,
                sectionsService,
                questionnaireService,
                workItemService,
                bucketName,
                appBaseUrl,
                storage,
                objectMapper,
                stateDBService,
                ticketTemplateDBService,
                filesService,
                mentionsService, questionnairesNotificationService, templateService);
        mvc = MockMvcBuilders
                .standaloneSetup(new QuestionnairesController(srv, eventsClient, DefaultObjectMapper.get()))
                .build();

        ArgumentCaptor<Questionnaire> questionnaireCaptor = ArgumentCaptor.forClass(Questionnaire.class);
        Questionnaire questionnaire = Questionnaire.builder()
                .state(State.CREATED)
                .answered(0)
                .createdAt(0L)
                .messageSent(true)
                .priority(Severity.HIGH)
                .questionnaireTemplateId("1")
                .targetEmail("target@test.io")
                .senderEmail("sender@test.io")
                .score(0)
                .workItemId("1")
                .totalQuestions(0)
                .totalPossibleScore(0)
                .bucketName(bucketName)
                .bucketPath("bucketPath")
                .build();
        List<UUID> questionIds = List.of(
                UUID.fromString("66bb6982-2ab3-4948-b8d8-7ed689276e5e"),
                UUID.fromString("153955a8-dc93-4256-a8af-427647eb84fc"),
                UUID.fromString("5a53736c-09d0-4cb1-8f34-c6066abe9236"));
        doReturn("1").when(questionnaireService).insert(eq("test"), questionnaireCaptor.capture());
        doReturn(Optional.of(QuestionnaireTemplate.builder()
                .id("063b6fc1-bbbe-46a9-8ce2-571350d0a343")
                .sections(questionIds)
                .lowRiskBoundary(10)
                .midRiskBoundary(30)
                .name("Template1")
                .build())).when(qTemplateDBService).get(eq("test"), eq("063b6fc1-bbbe-46a9-8ce2-571350d0a343"));
        when(msgTemplateService.get(eq("test"), eq("75cd01d2-bbbe-46a9-8ce2-571350d0a343"))).thenReturn(Optional.of(MessageTemplate.builder()
                .emailSubject("emailSubject")
                .build()));
        when(sectionsService.getBatch(eq("test"), eq(questionIds))).thenReturn(new ArrayList<>(List.of(
                Section.builder().id("153955a8-dc93-4256-a8af-427647eb84fc").questions(Collections.emptyList()).build(),
                Section.builder().id("66bb6982-2ab3-4948-b8d8-7ed689276e5e").questions(Collections.emptyList()).build(),
                Section.builder().id("5a53736c-09d0-4cb1-8f34-c6066abe9236").questions(Collections.emptyList()).build())));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[0]);
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/questionnaires")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ResourceUtils.getResourceAsString("questionnaire/questionnaire_post.json")))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        verify(questionnaireService, times(1)).insert(anyString(), any());
        // verify(questionnaireService, times(1)).update(anyString(), any());
        verify(storage, times(1)).create(any(BlobInfo.class), any(byte[].class));
        verify(sectionsService, times(1)).getBatch(anyString(), anyList());
        verify(qTemplateDBService, times(1)).get(anyString(), anyString());
        Assert.assertEquals(1, questionnaireCaptor.getAllValues().size());
        Assert.assertNotNull(questionnaireCaptor.getValue().getId());
        Assert.assertNotNull(UUID.fromString(questionnaireCaptor.getValue().getId()));
        Assert.assertEquals(questionnaire.getState(), questionnaireCaptor.getValue().getState());
    }

    @Test
    public void testGetQuestionnaire() throws Exception {
        QuestionnaireTemplateService qTemplateService = Mockito.mock(QuestionnaireTemplateService.class);
        SectionsService sectionsService = Mockito.mock(SectionsService.class);
        QuestionnaireDBService questionnaireService = Mockito.mock(QuestionnaireDBService.class);
        MsgTemplateService msgTemplateService = Mockito.mock(MsgTemplateService.class);
        Blob blob = Mockito.mock(Blob.class);
        QuestionnaireService srv = new QuestionnaireService(
                qTemplateService,
                msgTemplateService,
                integrationService,
                notificationService,
                sectionsService,
                questionnaireService,
                workItemService,
                bucketName,
                appBaseUrl,
                storage,
                objectMapper,
                stateDBService,
                ticketTemplateDBService,
                filesService,
                mentionsService, questionnairesNotificationService, templateService);
        mvc = MockMvcBuilders
                .standaloneSetup(new QuestionnairesController(srv, eventsClient, DefaultObjectMapper.get()))
                .build();
        ArgumentCaptor<String> qIdCaptor = ArgumentCaptor.forClass(String.class);
        QuestionnaireDetails questionnaire = QuestionnaireDetails.questionnaireDetailsBuilder()
                .id("66bb6982-2ab3-4948-b8d8-7ed689276e5e")
                .state(State.CREATED)
                .answered(0)
                .createdAt(0L)
                .messageSent(true)
                .priority(Severity.HIGH)
                .questionnaireTemplateId("1")
                .targetEmail("target@test.io")
                .senderEmail("sender@test.io")
                .score(0)
                .workItemId("1")
                .totalQuestions(0)
                .totalPossibleScore(0)
                .bucketName(bucketName)
                .bucketPath("bucketPath")
                .build();
        when(questionnaireService.getDetails(eq("test"), qIdCaptor.capture())).thenReturn(Optional.of(questionnaire));
        when(objectMapper.readValue(anyString(), eq(GcsQuestionnaireDTO.class))).thenReturn(GcsQuestionnaireDTO.builder().build());
        when(storage.get(anyString(), anyString())).thenReturn(blob);
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/questionnaires/66bb6982-2ab3-4948-b8d8-7ed689276e5e"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
        verify(questionnaireService, times(1)).getDetails(eq("test"), eq("66bb6982-2ab3-4948-b8d8-7ed689276e5e"));
        verify(storage, times(1)).get(eq(bucketName), eq("bucketPath"));
        verify(objectMapper, times(1)).readValue(anyString(), eq(GcsQuestionnaireDTO.class));
        verify(blob, times(1)).downloadTo(any(ByteArrayOutputStream.class));
    }

    @Test
    public void testUpdateQuestionnaire() throws Exception {
        QuestionnaireTemplateService qTemplateService = new QuestionnaireTemplateService(tagItemService, qTemplateDBService);
        SectionsService sectionsService = Mockito.mock(SectionsService.class);
        QuestionnaireDBService questionnaireService = Mockito.mock(QuestionnaireDBService.class);
        MsgTemplateService msgTemplateService = Mockito.mock(MsgTemplateService.class);
        Blob blob = Mockito.mock(Blob.class);
        QuestionnaireService srv = new QuestionnaireService(
                qTemplateService,
                msgTemplateService,
                integrationService,
                notificationService,
                sectionsService,
                questionnaireService,
                workItemService,
                bucketName,
                appBaseUrl,
                storage,
                objectMapper,
                stateDBService,
                ticketTemplateDBService,
                filesService,
                mentionsService, questionnairesNotificationService, templateService);
        mvc = MockMvcBuilders
                .standaloneSetup(new QuestionnairesController(srv, eventsClient, DefaultObjectMapper.get()))
                .build();
        ArgumentCaptor<Questionnaire> questionnaireCaptor = ArgumentCaptor.forClass(Questionnaire.class);
        ArgumentCaptor<String> qIdCaptor = ArgumentCaptor.forClass(String.class);
        QuestionnaireDetails questionnaire = QuestionnaireDetails.questionnaireDetailsBuilder()
                .id("76bb6982-2ab3-4948-b8d8-7ed689276e5e")
                .state(State.CREATED)
                .answered(0)
                .createdAt(0L)
                .messageSent(true)
                .priority(Severity.HIGH)
                .questionnaireTemplateId("1")
                .targetEmail("target@test.io")
                .senderEmail("sender@test.io")
                .score(0)
                .workItemId("1")
                .totalQuestions(0)
                .totalPossibleScore(0)
                .bucketName(bucketName)
                .bucketPath("bucketPath")
                .build();
        List<UUID> questionIds = List.of(
                UUID.fromString("66bb6982-2ab3-4948-b8d8-7ed689276e5e"),
                UUID.fromString("153955a8-dc93-4256-a8af-427647eb84fc"),
                UUID.fromString("5a53736c-09d0-4cb1-8f34-c6066abe9236"));
        // doReturn("1").when(questionnaireService).insert(eq("test"), questionnaireCaptor.capture());
        doReturn(Optional.of(QuestionnaireTemplate.builder()
                .id("063b6fc1-bbbe-46a9-8ce2-571350d0a343")
                .sections(questionIds)
                .lowRiskBoundary(10)
                .midRiskBoundary(30)
                .name("Template1")
                .build())).when(qTemplateDBService).get(eq("test"), eq("063b6fc1-bbbe-46a9-8ce2-571350d0a343"));
        doReturn(Optional.of(questionnaire)).when(questionnaireService).get(eq("test"), qIdCaptor.capture());
        doReturn(true).when(questionnaireService).update(eq("test"), questionnaireCaptor.capture());
        when(msgTemplateService.get(eq("test"), eq("75cd01d2-bbbe-46a9-8ce2-571350d0a343"))).thenReturn(Optional.of(MessageTemplate.builder()
                .emailSubject("emailSubject")
                .build()));
        when(sectionsService.getBatch(eq("test"), eq(questionIds))).thenReturn(List.of(
                Section.builder().id("66bb6982-2ab3-4948-b8d8-7ed689276e5e").questions(Collections.emptyList()).build(),
                Section.builder().id("153955a8-dc93-4256-a8af-427647eb84fc").questions(Collections.emptyList()).build(),
                Section.builder().id("5a53736c-09d0-4cb1-8f34-c6066abe9236").questions(Collections.emptyList()).build()));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[0]);
        when(storage.get(anyString(), anyString(), any())).thenReturn(blob);

        mvc.perform(asyncDispatch(mvc.perform(
                put("/internal/v1/tenants/test/questionnaires/76bb6982-2ab3-4948-b8d8-7ed689276e5e?submitter=test@test.test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ResourceUtils.getResourceAsString("questionnaire/questionnaire_put.json")))
                .andReturn()))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        when(questionnaireService.getDetails(eq("test"), eq("76bb6982-2ab3-4948-b8d8-7ed689276e5e"))).thenReturn(Optional.of(
                questionnaire.toBuilder()
                        .id(questionnaireCaptor.getValue().getId())
                        .state(questionnaireCaptor.getValue().getState()).build()));
        when(objectMapper.readValue(anyString(), eq(GcsQuestionnaireDTO.class))).thenReturn(GcsQuestionnaireDTO.builder().build());
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/questionnaires/76bb6982-2ab3-4948-b8d8-7ed689276e5e"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
        verify(questionnaireService, times(1)).get(anyString(), anyString());
        Assert.assertEquals(State.COMPLETED, questionnaireCaptor.getValue().getState());
        Assert.assertEquals("76bb6982-2ab3-4948-b8d8-7ed689276e5e", qIdCaptor.getValue());
    }

    @Test
    public void testUpdateQuestionnaireValidation() throws Exception {
        QuestionnaireTemplateService qTemplateService = new QuestionnaireTemplateService(tagItemService, qTemplateDBService);
        SectionsService sectionsService = Mockito.mock(SectionsService.class);
        QuestionnaireDBService questionnaireService = Mockito.mock(QuestionnaireDBService.class);
        MsgTemplateService msgTemplateService = Mockito.mock(MsgTemplateService.class);
        Blob blob = Mockito.mock(Blob.class);
        QuestionnaireService srv = new QuestionnaireService(
                qTemplateService,
                msgTemplateService,
                integrationService,
                notificationService,
                sectionsService,
                questionnaireService,
                workItemService,
                bucketName,
                appBaseUrl,
                storage,
                objectMapper,
                stateDBService,
                ticketTemplateDBService,
                filesService,
                mentionsService, questionnairesNotificationService, templateService);
        mvc = MockMvcBuilders
                .standaloneSetup(new QuestionnairesController(srv, eventsClient, DefaultObjectMapper.get()))
                .build();
        ArgumentCaptor<Questionnaire> questionnaireCaptor = ArgumentCaptor.forClass(Questionnaire.class);
        ArgumentCaptor<String> qIdCaptor = ArgumentCaptor.forClass(String.class);
        QuestionnaireDetails questionnaire = QuestionnaireDetails.questionnaireDetailsBuilder()
                .id("76bb6982-2ab3-4948-b8d8-7ed689276e5e")
                .state(State.CREATED)
                .answered(0)
                .createdAt(0L)
                .messageSent(true)
                .priority(Severity.HIGH)
                .questionnaireTemplateId("1")
                .targetEmail("target@test.io")
                .senderEmail("sender@test.io")
                .score(0)
                .workItemId("1")
                .totalQuestions(0)
                .totalPossibleScore(0)
                .bucketName(bucketName)
                .bucketPath("bucketPath")
                .build();
        List<UUID> questionIds = List.of(
                UUID.fromString("66bb6982-2ab3-4948-b8d8-7ed689276e5e"),
                UUID.fromString("153955a8-dc93-4256-a8af-427647eb84fc"),
                UUID.fromString("5a53736c-09d0-4cb1-8f34-c6066abe9236"));
        // doReturn("1").when(questionnaireService).insert(eq("test"), questionnaireCaptor.capture());
        doReturn(Optional.of(QuestionnaireTemplate.builder()
                .id("063b6fc1-bbbe-46a9-8ce2-571350d0a343")
                .sections(questionIds)
                .lowRiskBoundary(10)
                .midRiskBoundary(30)
                .name("Template1")
                .build())).when(qTemplateDBService).get(eq("test"), eq("063b6fc1-bbbe-46a9-8ce2-571350d0a343"));
        doReturn(Optional.of(questionnaire)).when(questionnaireService).get(eq("test"), qIdCaptor.capture());
        doReturn(true).when(questionnaireService).update(eq("test"), questionnaireCaptor.capture());
        when(msgTemplateService.get(eq("test"), eq("75cd01d2-bbbe-46a9-8ce2-571350d0a343"))).thenReturn(Optional.of(MessageTemplate.builder()
                .emailSubject("emailSubject")
                .build()));
        when(sectionsService.getBatch(eq("test"), eq(questionIds))).thenReturn(List.of(
                Section.builder().id("66bb6982-2ab3-4948-b8d8-7ed689276e5e").questions(Collections.emptyList()).build(),
                Section.builder().id("153955a8-dc93-4256-a8af-427647eb84fc").questions(Collections.emptyList()).build(),
                Section.builder().id("5a53736c-09d0-4cb1-8f34-c6066abe9236").questions(Collections.emptyList()).build()));
        when(objectMapper.writeValueAsBytes(any())).thenReturn(new byte[0]);
        when(storage.get(anyString(), anyString(), any())).thenReturn(blob);

        mvc.perform(
                put("/internal/v1/tenants/test/questionnaires/76bb6982-2ab3-4948-b8d8-7ed689276e5e'OR id not null'?submitter=test@test.test"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetQuestionnaireValidation() throws Exception {
        QuestionnaireTemplateService qTemplateService = Mockito.mock(QuestionnaireTemplateService.class);
        SectionsService sectionsService = Mockito.mock(SectionsService.class);
        QuestionnaireDBService questionnaireService = Mockito.mock(QuestionnaireDBService.class);
        MsgTemplateService msgTemplateService = Mockito.mock(MsgTemplateService.class);
        Blob blob = Mockito.mock(Blob.class);
        QuestionnaireService srv = new QuestionnaireService(
                qTemplateService,
                msgTemplateService,
                integrationService,
                notificationService,
                sectionsService,
                questionnaireService,
                workItemService,
                bucketName,
                appBaseUrl,
                storage,
                objectMapper,
                stateDBService,
                ticketTemplateDBService,
                filesService,
                mentionsService, questionnairesNotificationService, templateService);
        mvc = MockMvcBuilders
                .standaloneSetup(new QuestionnairesController(srv, eventsClient, DefaultObjectMapper.get()))
                .build();
        ArgumentCaptor<String> qIdCaptor = ArgumentCaptor.forClass(String.class);
        QuestionnaireDetails questionnaire = QuestionnaireDetails.questionnaireDetailsBuilder()
                .id("66bb6982-2ab3-4948-b8d8-7ed689276e5e")
                .state(State.CREATED)
                .answered(0)
                .createdAt(0L)
                .messageSent(true)
                .priority(Severity.HIGH)
                .questionnaireTemplateId("1")
                .targetEmail("target@test.io")
                .senderEmail("sender@test.io")
                .score(0)
                .workItemId("1")
                .totalQuestions(0)
                .totalPossibleScore(0)
                .bucketName(bucketName)
                .bucketPath("bucketPath")
                .build();
        when(questionnaireService.getDetails(eq("test"), qIdCaptor.capture())).thenReturn(Optional.of(questionnaire));
        when(objectMapper.readValue(anyString(), eq(GcsQuestionnaireDTO.class))).thenReturn(GcsQuestionnaireDTO.builder().build());
        when(storage.get(anyString(), anyString())).thenReturn(blob);
        mvc.perform(asyncDispatch(mvc.perform(get("/internal/v1/tenants/test/questionnaires/66bb6982-2ab3-4948-b8d8-7ed689276e5e"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        mvc.perform(
                put("/internal/v1/tenants/test/questionnaires/66bb6982-2ab3-4948-b8d8-7ed689276e5e'OR id not null'"))
                .andExpect(status().isBadRequest());

    }
}
