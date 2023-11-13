package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.questionnaire.Answer;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.SectionResponse;
import io.levelops.commons.databases.models.filters.DateFilter;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.databases.services.QuestionnaireDBService;
import io.levelops.commons.databases.services.SectionsService;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.databases.services.WorkItemDBService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.notification.services.NotificationService;
import io.levelops.services.TemplateService;
import io.levelops.uploads.services.FilesService;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
@Log4j2
public class QuestionnaireServiceTest {
    @Mock
    private QuestionnaireTemplateService questionnaireTemplateService;
    @Mock
    private MsgTemplateService msgTemplateService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SectionsService sectionsService;
    @Mock
    private QuestionnaireDBService questionnaireDBService;
    @Mock
    private WorkItemDBService workItemDBService;
    @Mock
    private TicketTemplateDBService ticketTemplateDBService;

    private String appBaseUrl = "appBaseUrl";
    private String bucketName = "bucketName";
    @Mock
    private Storage storage;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StateDBService stateDBService;
    @Mock
    private FilesService filesService;
    @Mock
    private MentionsService mentionsService;
    @Mock
    private QuestionnairesNotificationService questionnairesNotificationService;
    @Mock
    private TemplateService templateService;

    private QuestionnaireService questionnaireService = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        questionnaireService = new QuestionnaireService(questionnaireTemplateService, msgTemplateService, integrationService, notificationService,
                sectionsService, questionnaireDBService, workItemDBService, bucketName, appBaseUrl, storage, objectMapper, stateDBService, ticketTemplateDBService,
                filesService, mentionsService, questionnairesNotificationService, templateService);
        List<UUID> sections = List.of(UUID.randomUUID(), UUID.randomUUID());
        Mockito.when(questionnaireTemplateService.read("company","3")).thenReturn(
                java.util.Optional.ofNullable(QuestionnaireTemplate.builder()
                        .id("3")
                        .build())
        );
        Mockito.when(sectionsService.getBatch("company",sections))
                .thenReturn(List.of(Section.builder()
                .id(UUID.randomUUID().toString()).build()));
    }

    @Test
    public void name() throws JsonProcessingException {
        DateFilter dateFilter = DefaultObjectMapper.get().readValue("{\"$gt\":\"1601535600\",\"$lt\":1604213999}", DateFilter.class);
        DefaultObjectMapper.prettyPrint(dateFilter);
    }

    @Test
    public void testQuestionnaireUpdateMissingGeneration() throws SQLException, JsonProcessingException {
        try {
            questionnaireService.questionnaireUpdate("company", "submitter",
                    QuestionnaireDTO.builder().build());
        } catch (ResponseStatusException e) {
            Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            Assert.assertEquals("Updated Questionnaire does not contain generation!", e.getReason());
        }
    }

    @Test
    public void testQuestionnaireUpdateInvalidResponse() throws SQLException, JsonProcessingException {
        SectionResponse sectionResponse1 = SectionResponse.builder()
                .build();
        SectionResponse sectionResponse2 = SectionResponse.builder()
                .answers(List.of(Answer.builder().build()))
                .build();
        SectionResponse sectionResponse3 = SectionResponse.builder()
                .answers(List.of(
                        Answer.builder().build(),
                        Answer.builder()
                                .responses(
                                        List.of(
                                                new Answer.Response(UUID.randomUUID().toString(), null, 0, "file", "file1", 100L, "viraj"),
                                                new Answer.Response("junk", null, 0, "single-select", null, 100L, "viraj"),
                                                new Answer.Response("junk", null, 0, "file", "file1", 100L, "viraj")
                                        )
                                )
                                .build()
                ))
                .build();

        try {
            questionnaireService.questionnaireUpdate("company", "submitter",
                    QuestionnaireDTO.builder()
                            .generation("1617655779941793")
                            .answers(List.of(sectionResponse1, sectionResponse2, sectionResponse3))
                            .build());
            Assert.fail("ResponseStatusException expected");
        } catch (ResponseStatusException e) {
            Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            Assert.assertEquals("response type file expects value in UUID format, actual value junk is invalid", e.getReason());
        }
    }
    @Test
    public void testCreateQuestionnaire() throws JsonProcessingException {
        SectionResponse sectionResponse1 = SectionResponse.builder()
                .build();
        SectionResponse sectionResponse2 = SectionResponse.builder()
                .answers(List.of(Answer.builder().build()))
                .build();
        SectionResponse sectionResponse3 = SectionResponse.builder()
                .answers(List.of(
                        Answer.builder().build(),
                        Answer.builder()
                                .responses(
                                        List.of(
                                                new Answer.Response(UUID.randomUUID().toString(), null, 0, "file", "file1", 100L, "viraj"),
                                                new Answer.Response(UUID.randomUUID().toString(), null, 0, "single-select", null, 100L, "viraj"),
                                                new Answer.Response(UUID.randomUUID().toString(), null, 0, "file", "file1", 100L, "viraj")
                                        )
                                )
                                .build()
                ))
                .build();
        try {
            String questionnaire = questionnaireService.createQuestionnaire("company",
                    QuestionnaireDTO.builder()
                            .id("1")
                            .questionnaireTemplateId("3")
                            .generation("1617655779941793")
                            .answers(List.of(sectionResponse1, sectionResponse2, sectionResponse3))
                            .build());
            assertThat(questionnaire).isNotEmpty();
        } catch (SQLException | BadRequestException e) {
            log.debug("Error in executing sql....");
        }
    }
}