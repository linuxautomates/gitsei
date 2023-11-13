package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.Strings;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.MessageTemplate.TemplateType;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.NotificationRequestorType;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.QuestionnaireNotificationRequest;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.questionnaire.Answer;
import io.levelops.commons.databases.models.database.questionnaire.Answer.Comment;
import io.levelops.commons.databases.models.database.questionnaire.GcsQuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireListItemDTO;
import io.levelops.commons.databases.models.database.questionnaire.SectionResponse;
import io.levelops.commons.databases.models.filters.DateFilter;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.filters.QuestionnaireFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.QuestionnaireDetails;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.databases.services.QuestionnaireDBService;
import io.levelops.commons.databases.services.SectionsService;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.databases.services.WorkItemDBService;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.UUIDUtils;
import io.levelops.internal_api.services.handlers.LevelOpsLinkUtils;
import io.levelops.internal_api.services.utils.ServiceUtils;
import io.levelops.notification.services.NotificationService;
import io.levelops.services.TemplateService;
import io.levelops.uploads.services.FilesService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.Questionnaire.State.COMPLETED;

@Log4j2
@Service
public class QuestionnaireService {
    private static final String ASSESSMENT_LINK_FORMAT = "%s/#/admin/answer-questionnaire-page?questionnaire=%s&tenant=%s";
    private static final String WORKITEM_LINK_FORMAT = "%s/#/admin/workitems/details?workitem=%s";
    private static final String ANSWERS_PATH = "/%s/questionnaires/%s/answers/%s.json";
    private static final String LEVELOPS_BRAND_NAME = "Propelo";
    private static final String SLACK_LEVELOPS_BRAND_NAME_HEADER = String.format("_-Powered by %s-_\n", LEVELOPS_BRAND_NAME);

    private final MessageTemplate commentMessageTemplate = MessageTemplate.builder()
            .type(TemplateType.EMAIL)
            .emailSubject("You were mentioned in $issue_id.")
            .message("A new assessment comment, on which you are mentioned, has been added:<br /><br /><q>$text</q><br/><br/><a href=\"$assessment_link\">Go To Assessment</a>")
            .build();


    // region Data Members
    private final QuestionnaireTemplateService questionnaireTemplateService;
    private final MsgTemplateService msgTemplateService;
    private final IntegrationService integrationService;
    private final NotificationService notificationService;
    private final SectionsService sectionsService;
    private final QuestionnaireDBService questionnaireDBService;
    private final WorkItemDBService workItemDBService;
    private final TicketTemplateDBService ticketTemplateDBService;

    private final String appBaseUrl;
    private final String bucketName;
    private final Storage storage;
    private final ObjectMapper objectMapper;
    private final StateDBService stateDBService;
    private final FilesService filesService;
    private final MentionsService mentionsService;
    private final QuestionnairesNotificationService questionnairesNotificationService;
    private final TemplateService templateService;
    // endregion

    // region CSTOR
    @Autowired
    public QuestionnaireService(QuestionnaireTemplateService questionnaireTemplateService,
                                MsgTemplateService msgTemplateService,
                                IntegrationService integrationService,
                                NotificationService notificationService,
                                SectionsService sectionsService,
                                QuestionnaireDBService questionnaireDBService,
                                WorkItemDBService workItemDBService,
                                @Value("${UPLOADS_BUCKET_NAME:levelops-uploads}") final String bucketName,
                                @Value("${APP_BASE_URL:https://app.propelo.ai}") final String appBaseUrl,
                                Storage storage,
                                ObjectMapper objectMapper,
                                StateDBService stateDBService,
                                final TicketTemplateDBService ticketTemplateDBService,
                                final FilesService filesService,
                                final MentionsService mentionsService, QuestionnairesNotificationService questionnairesNotificationService, TemplateService templateService) {
        this.questionnaireTemplateService = questionnaireTemplateService;
        this.msgTemplateService = msgTemplateService;
        this.integrationService = integrationService;
        this.notificationService = notificationService;
        this.sectionsService = sectionsService;
        this.questionnaireDBService = questionnaireDBService;
        this.workItemDBService = workItemDBService;
        this.bucketName = bucketName;
        this.appBaseUrl = appBaseUrl;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.stateDBService = stateDBService;
        this.ticketTemplateDBService = ticketTemplateDBService;
        this.filesService = filesService;
        this.mentionsService = mentionsService;
        this.questionnairesNotificationService = questionnairesNotificationService;
        this.templateService = templateService;
    }

    public String createQuestionnaire(String company, QuestionnaireDTO questionnaire)
            throws SQLException, JsonProcessingException, BadRequestException {
        log.debug("questionnaire {}", questionnaire);
        validateSectionResponses(questionnaire);

        boolean isMain = Boolean.TRUE.equals(questionnaire.getMain());
        // Get template to retrieve the questions ids
        QuestionnaireTemplate questionnaireTemplate = questionnaireTemplateService
                .read(company, questionnaire.getQuestionnaireTemplateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Incorrect template id. Template with id '" + questionnaire.getQuestionnaireTemplateId()
                                + "' could not be found."));
        log.debug("questionnaireTemplate {}", questionnaireTemplate);

        var questionnaireId = UUID.randomUUID();
        var answersPath = String.format(ANSWERS_PATH, company, questionnaireId.toString(), UUID.randomUUID().toString());

        // Upload answers and assignment msg to GCS. also sort to make sure questions are in the right order.
        // using an arraylist so it can be cleared later.
        List<Section> sections = new ArrayList<>(sectionsService.getBatch(
                company, questionnaireTemplate.getSections()));
        log.debug("sections before {}", sections);
        Map<UUID, Section> questionsMap = new ConcurrentHashMap<>();
        Integer questionCount = sections.parallelStream()
                .map(qn -> {
                    questionsMap.put(UUID.fromString(qn.getId()), qn);
                    if (qn.getQuestions() != null) {
                        return qn.getQuestions().size();
                    }
                    return 0;
                }).reduce(0, Integer::sum);
        sections.clear();
        questionnaireTemplate.getSections().forEach(q -> {
            if (q != null && questionsMap.containsKey(q)) {
                sections.add(questionsMap.get(q));
            }
        });
        log.debug("sections after {}", sections);

        GcsQuestionnaireDTO blob = GcsQuestionnaireDTO.builder()
                .sectionResponses(questionnaire.getAnswers())
                .assignmentMsg("")
                .sections(sections)
                .build();
        log.debug("blob: {}", blob);
        storage.create(
                BlobInfo.newBuilder(bucketName, answersPath).build(),
                objectMapper.writeValueAsBytes(blob));

        // Assemble Questionnaire object
        Questionnaire q = Questionnaire.builder()
                .bucketName(bucketName)
                .bucketPath(answersPath)
                .questionnaireTemplateId(questionnaire.getQuestionnaireTemplateId())
                .totalQuestions(questionCount)
                .totalPossibleScore(QuestionnaireDTO.calculateMaxScore(sections))
                .answered(questionnaire.getAnsweredQuestions())
                .workItemId(questionnaire.getWorkItemId())
                .targetEmail(questionnaire.getTargetEmail())
                .senderEmail(questionnaire.getSenderEmail())
                .score(questionnaire.getCurrentScore())
                .priority(Severity.UNKNOWN)
                .productId(questionnaire.getProductId())
                .messageSent(false)
                .id(questionnaireId.toString())
                .state(Questionnaire.State.CREATED)
                .main(isMain)
                .kbIds((questionnaire.getKbIds() != null) ? questionnaire.getKbIds() : Collections.emptyList())
                .build();
        // This inserts with the id.
        questionnaireDBService.insert(company, q);
        //Questionnaire Detail set id
        QuestionnaireDTO questionnaireUpdated = questionnaire.toBuilder().id(questionnaireId.toString()).questionnaireTemplateName(questionnaireTemplate.getName()).sections(sections).build();
        log.debug("before questionnaireUpdated {}", questionnaireUpdated);

        //Everything below this is for notification (mainly slack)
        Optional<WorkItem> item = workItemDBService.get(company, questionnaire.getWorkItemId());

        //ToDo: VA Fix Allow sending Questionnaire notification, even if WI does not exist
        if (item.isEmpty()) {
            log.info("Cannot send Questionnaire created notification, Work Item not found! wi id {}", questionnaire.getWorkItemId());
            return questionnaireId.toString();
        }
        var optionalTemplate = ticketTemplateDBService.get(company, item.get().getTicketTemplateId());
        if (optionalTemplate.isEmpty()) {
            return questionnaireId.toString();
        }
        item.get().getAssignees().forEach(assignee ->
                notify(company, item.get(), optionalTemplate.get(), EventType.ASSESSMENT_CREATED,
                        assignee.getUserEmail(), questionnaireUpdated)
        );

        return questionnaireId.toString();
    }

    private QuestionnaireDTO getQuestionnaireDetails(QuestionnaireDetails questionnaire) throws JsonProcessingException {
        // Download the answers.
        ByteArrayOutputStream byos = new ByteArrayOutputStream();

        String generation = null;
        List<SectionResponse> answers = Collections.emptyList();
        List<Section> sections = Collections.emptyList();
        String assignmentMessage = "";
        String comments = "";
        if (!Strings.isNullOrEmpty(questionnaire.getBucketPath())) {
            Blob blob = storage.get(bucketName, questionnaire.getBucketPath());
            blob.downloadTo(byos);
            GcsQuestionnaireDTO data = objectMapper.readValue(
                    new String(byos.toByteArray(), StandardCharsets.UTF_8),
                    GcsQuestionnaireDTO.class);
            answers = data.getSectionResponses();
            assignmentMessage = data.getAssignmentMsg();
            sections = data.getSections();
            comments = data.getComments();
            generation = blob.getGeneration().toString();
        }
        // Assemble response object and respond.
        return QuestionnaireDTO.builder()
                .id(questionnaire.getId())
                .workItemId(questionnaire.getWorkItemId())
                .questionnaireTemplateId(questionnaire.getQuestionnaireTemplateId())
                .questionnaireTemplateName(questionnaire.getQuestionnaireTemplateName())
                .totalQuestions(questionnaire.getTotalQuestions())
                .answeredQuestions(questionnaire.getAnswered())
                .senderEmail(questionnaire.getSenderEmail())
                .targetEmail(questionnaire.getTargetEmail())
                .integrationApplication(questionnaire.getIntegrationApplication())
                .integrationUrl(questionnaire.getIntegrationUrl())
                .artifact(questionnaire.getArtifact())
                .notificationMessage(assignmentMessage)
                .answers(answers)
                .productId(questionnaire.getProductId())
                .comments(comments)
                .generation(generation)
                .currentScore(questionnaire.getScore())
                .priority(questionnaire.getPriority())
                .sections(sections)
                .messageSent(questionnaire.getMessageSent())
                .totalScore(questionnaire.getTotalPossibleScore())
                .updatedAt(questionnaire.getUpdatedAt())
                .completed(questionnaire.getState() == COMPLETED)
                .tagIds(questionnaire.getTagIds())
                .state(questionnaire.getState())
                .main(questionnaire.getMain())
                .kbIds((questionnaire.getKbIds() != null) ? questionnaire.getKbIds() : Collections.emptyList())
                .build();

    }

    public QuestionnaireDTO questionnaireDetails(final String company, final UUID id)
            throws SQLException, JsonProcessingException, NotFoundException {
        var questionnaire = questionnaireDBService.getDetails(company, id.toString()).orElseThrow(() ->
                new NotFoundException("Questionnaire with id '" + id + "' not found."));
        return getQuestionnaireDetails(questionnaire);
    }

    private void validateSectionResponses(final QuestionnaireDTO questionnaire) {
        if(questionnaire == null) {
            return;
        }
        org.apache.commons.collections4.CollectionUtils.emptyIfNull(questionnaire.getAnswers()).stream()
                .filter(sectionResponse -> org.apache.commons.collections4.CollectionUtils.isNotEmpty(sectionResponse.getAnswers()))
                .flatMap(sectionResponse -> sectionResponse.getAnswers().stream())
                .filter(answer -> org.apache.commons.collections4.CollectionUtils.isNotEmpty(answer.getResponses()))
                .flatMap(answer -> answer.getResponses().stream())
                .forEach(response -> {
                    if (("file".equals(response.getType())) && (StringUtils.isNotBlank(response.getValue()))) {
                        try {
                            UUID.fromString(response.getValue());
                        } catch (IllegalArgumentException e) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "response type file expects value in UUID format, actual value " + response.getValue() + " is invalid");
                        }
                    }
                });
    }

    public String questionnaireUpdate(final String company, final String submitter,
                                      final QuestionnaireDTO questionnaire)
            throws SQLException, JsonProcessingException {
        Long generation = null;
        try {
            generation = Long.valueOf(questionnaire.getGeneration());
        } catch (NumberFormatException e) {
            //This includes npe ^^
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Updated Questionnaire does not contain generation!");
        }
        validateSectionResponses(questionnaire);

        String id = questionnaire.getId();
        Questionnaire originalQn = questionnaireDBService.get(company, id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Questionnaire with id '" + id + "' not found."));
        try {
            processComments(company, submitter, questionnaire, originalQn);
        } catch (IOException e) {
            log.error("Unable to process mentions in comments.", e);
        }

        try {
            storage.create(
                    BlobInfo.newBuilder(
                            originalQn.getBucketName(),
                            originalQn.getBucketPath(),
                            Long.valueOf(generation))
                            .build(),
                    objectMapper.writeValueAsBytes(
                            GcsQuestionnaireDTO.builder()
                                    .sectionResponses(questionnaire.getAnswers())
                                    .assignmentMsg(questionnaire.getNotificationMessage())
                                    .sections(questionnaire.getSections())
                                    .comments(questionnaire.getComments())
                                    .build()),
                    Storage.BlobTargetOption.generationMatch());
        } catch (StorageException exception) {
            Throwable rootCause = ExceptionUtils.getRootCause(exception);
            if (rootCause instanceof GoogleJsonResponseException
                    && (((GoogleJsonResponseException) rootCause).getStatusCode() == 412)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Questionnaire in the cloud has a different generation.");
            }
        }
        // Assemble Questionnaire object
        int score = QuestionnaireDTO.calculateScore(questionnaire.getAnswers());
        var qt = questionnaireTemplateService.read(company, questionnaire.getQuestionnaireTemplateId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "QuestionnaireTemplate with id '" + questionnaire.getQuestionnaireTemplateId()
                                        + "' not found."));
        var maxScore = (questionnaire.getTotalScore() != null && questionnaire.getTotalScore() > 0) ?
                questionnaire.getTotalScore() : (CollectionUtils.isEmpty(questionnaire.getSections()) ?
                1 : QuestionnaireDTO.calculateMaxScore(questionnaire.getSections()));
        Questionnaire q = originalQn.toBuilder()
                .id(id)
                .workItemId(questionnaire.getWorkItemId())
                .questionnaireTemplateId(questionnaire.getQuestionnaireTemplateId())
                .score(score)
                .productId(questionnaire.getProductId())
                .priority(QuestionnaireDTO.calculatePriority(qt.getLowRiskBoundary(),
                        qt.getMidRiskBoundary(), maxScore, score))
                .answered(questionnaire.getAnsweredQuestions())
                .totalQuestions(questionnaire.getTotalQuestions())
                .totalPossibleScore(maxScore)
                .state(questionnaire.getCompleted() ? COMPLETED : Questionnaire.State.INCOMPLETE)
                .build();
        // Insert
        questionnaireDBService.update(company, q);
        Optional<WorkItem> item = workItemDBService.get(company, questionnaire.getWorkItemId());

        if (item.isEmpty()) {
            return id;
        }

        WorkItem originalWi = item.get();
        String stateId = originalWi.getStateId().toString();
        State state = stateDBService.get(company, stateId)
                .orElseThrow(() -> new SQLException("State not found! id = " + stateId));
        WorkItem.ItemStatus itemStatus = WorkItem.ItemStatus.fromString(state.getName());

        if (questionnaire.getCompleted() && itemStatus == WorkItem.ItemStatus.OPEN) {
            State inReviewState = stateDBService.getStateByName(
                    company, WorkItem.ItemStatus.IN_REVIEW.toString());
            workItemDBService.update(company,
                    originalWi.toBuilder().stateId(inReviewState.getId()).build());
        }
        if (questionnaire.getCompleted()) {
            var optionalTemplate = ticketTemplateDBService.get(company, item.get().getTicketTemplateId());
            if (optionalTemplate.isEmpty()) {
                return id;
            }
            item.get().getAssignees().forEach(assignee ->
                    notify(company, item.get(), optionalTemplate.get(), EventType.ASSESSMENT_SUBMITTED,
                            assignee.getUserEmail(), QuestionnaireDTO.builder().id(questionnaire.getId()).build())
            );
        }
        //Rebuild Cache when the Questionnaire
        rebuildQuestionnaireSlackMessageCacheAsync(company, id);
        return id;
    }

    /**
     * Processes the current comments and the previous ones using the mentions
     * service to notify new mentions.
     *
     * @param company       tenant
     * @param submitter     the person submitting the update to the questionnaire
     * @param questionnaire new questionnaire
     * @param originalQn    previous questionnaire
     * @throws IOException
     */
    private void processComments(final String company, final String submitter, QuestionnaireDTO questionnaire, Questionnaire originalQn)
            throws IOException {
        log.debug("Processing comments in section responses: company={}, answers={}", company, questionnaire.getAnswers());
        Map<String, List<Comment>> currentComments = questionnaire.getAnswers().stream()
                .flatMap(sectionResponse -> sectionResponse.getAnswers().stream())
                .filter(answer -> answer.getComments().size() > 0)
                .collect(Collectors.toMap(
                        Answer::getQuestionId,
                        Answer::getComments));
        log.info("section responses to process: total={}", currentComments.size());

        // assessment comments
        if (StringUtils.isNotBlank(questionnaire.getComments())) {
            log.debug("Adding assessment comment.");
            currentComments.put(questionnaire.getId(), List.of(new Comment(submitter, questionnaire.getComments(), Instant.now().toEpochMilli())));
        }

        var issueId = "an assessment";
        try {
            var workItem = workItemDBService.get(company, questionnaire.getWorkItemId());
            if (workItem.isPresent()) {
                issueId = workItem.get().getVanityId();
            }
        } catch (SQLException e) {
            log.warn("unable to get the workItem '{}'", questionnaire.getWorkItemId(), e);
        }
        Map<String, Object> values = Map.of(
                "issue_id", issueId,
                "assessment_id", questionnaire.getId(),
                "assessment_link", String.format(ASSESSMENT_LINK_FORMAT, appBaseUrl, questionnaire.getId(), company));
        if (originalQn == null || StringUtils.isBlank(originalQn.getBucketPath())) {
            log.info("no previous comments since there is no bucket path for previous results");
            mentionsService.notifyMentionsInText(
                    company,
                    questionnaire.getSenderEmail(),
                    currentComments,
                    commentMessageTemplate,
                    values);
            return;
        }

        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        storage.get(bucketName, originalQn.getBucketPath()).downloadTo(byos);
        GcsQuestionnaireDTO originalData = objectMapper.readValue(
                new String(byos.toByteArray(), StandardCharsets.UTF_8),
                GcsQuestionnaireDTO.class
        );
        Map<String, List<Comment>> previousComments = new HashMap<>();
        // old data assessment comments
        if (originalData != null && StringUtils.isNotBlank(originalData.getComments())) {
            log.debug("Adding old data assessment comment.");
            previousComments.put(questionnaire.getId(), List.of(new Comment(submitter, originalData.getComments(), 0L)));
        }
        if (originalData == null || originalData.getSectionResponses() == null || CollectionUtils.isEmpty(originalData.getSectionResponses())) {
            log.info("no previous comments since original data is empty");
            mentionsService.notifyMentionsInText(
                    company,
                    questionnaire.getSenderEmail(),
                    currentComments,
                    previousComments,
                    commentMessageTemplate,
                    values);
            return;
        }
        previousComments.putAll(originalData.getSectionResponses().stream()
                .flatMap(sectionResponse -> sectionResponse.getAnswers().stream())
                .filter(answer -> answer.getComments().size() > 0)
                .collect(Collectors.toMap(
                        Answer::getQuestionId,
                        Answer::getComments)));
        log.info("previous section responses to process: total={}", previousComments.size());

        mentionsService.notifyMentionsInText(
                company,
                questionnaire.getSenderEmail(),
                currentComments,
                previousComments,
                commentMessageTemplate,
                values);
    }

    private void notify(String company, WorkItem workitem, TicketTemplate ticketTemplate, EventType eventType,
                        String recipientEmail, QuestionnaireDTO assessment) {
        var notifications = ticketTemplate.getNotifyBy().getOrDefault(eventType, ticketTemplate.getNotifyBy().getOrDefault(EventType.ALL, null));
        if (CollectionUtils.isEmpty(notifications)) {
            log.warn("No notification configured for the event {}", eventType);
            return;
        }
        DbListResponse<MessageTemplate> records;
        try {
            records = msgTemplateService.list(company, 0, 100);
        } catch (Exception e1) {
            log.error("Unable to get templates.", e1);
            return;
        }
        if (records == null || CollectionUtils.isEmpty(records.getRecords())) {
            log.warn("No templates found");
            return;
        }

        final var comparableNotifications = notifications.stream().map(String::toLowerCase).collect(Collectors.toList());

        List<MessageTemplate> notificationTemplates = ServiceUtils.getMessageTemplates(records.getRecords(), comparableNotifications, eventType, ticketTemplate);
        if (CollectionUtils.isEmpty(notificationTemplates)) {
            notificationTemplates = ServiceUtils.getMessageTemplates(records.getRecords(), comparableNotifications, eventType);
        }
        log.debug("notificationTemplates {}", notificationTemplates);

        if (CollectionUtils.isEmpty(notificationTemplates)) {
            log.warn("No template found for the ticket '{}'", workitem.getId());
            return;
        }
        var values = new HashMap<String, Object>();
        values.put("title", workitem.getTitle());
        values.put("link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, workitem.getVanityId()));
        values.put("issue_id", workitem.getVanityId());
        values.put("issue_link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, workitem.getVanityId()));
        values.put("issue_title", workitem.getTitle());
        values.put("assessment_id", assessment.getId());
        values.put("assessment_link", String.format(ASSESSMENT_LINK_FORMAT, appBaseUrl, assessment.getId(), company));
        values.put("assessment_title", assessment.getQuestionnaireTemplateName());
        values.put("sender", assessment.getSenderEmail());

        notificationTemplates.forEach(notificationTemplate -> {
            log.debug("eventType {}, notificationTemplate.getType() {}", eventType, notificationTemplate.getType());
            if ((TemplateType.SLACK.equals(notificationTemplate.getType())) && (eventType == EventType.ASSESSMENT_CREATED)) {
                String bot = templateService.evaluateTemplate(notificationTemplate.getBotName(), values);
                log.debug("bot {}", bot);
                String content = templateService.evaluateTemplate(notificationTemplate.getMessage(), values);
                log.debug("content {}", content);
                String textWithHeader = SLACK_LEVELOPS_BRAND_NAME_HEADER + content;
                log.debug("textWithHeader {}", textWithHeader);

                QuestionnaireNotificationRequest questionnaireNotificationRequest = QuestionnaireNotificationRequest.builder()
                        .questionnaireId(UUID.fromString(assessment.getId())).recipients(List.of(recipientEmail)).mode(NotificationMode.SLACK)
                        .requestorType(NotificationRequestorType.USER).requestorName(null).requestorId(assessment.getSenderEmail())
                        .build();

                try {
                    questionnairesNotificationService.queueQuestionnaireNotification(company, questionnaireNotificationRequest, textWithHeader, bot, assessment);
                } catch (IOException e) {
                    log.error("Unable to send a notification to {} for {}...", recipientEmail, eventType, e);
                }
            } else {
                try {
                    notificationService.sendNotification(company, notificationTemplate, recipientEmail, values);
                } catch (IOException e) {
                    log.error("Unable to send a notification to {} for {}...", recipientEmail, eventType, e);
                }
            }
        });
    }

    public boolean questionnaireDelete(final String company, final String id) throws SQLException {
        Optional<Questionnaire> questionnaire = questionnaireDBService.deleteAndReturn(company, id);
        if (questionnaire.isPresent()) {
            try {
                filesService.deleteEverythingUnderComponent(
                        company, "", RoleType.ADMIN, ComponentType.ASSESSMENT, questionnaire.get().getId());
            } catch (IOException e) {
                log.error("Didn't delete all the files stored for the assessment {}", id, e);
            }
            storage.delete(BlobId.of(questionnaire.get().getBucketName(), questionnaire.get().getBucketPath()));
            return true;
        }
        log.error("QuestionnaireService: questionnaireDelete cannot find questionnaire id {} for company: {}", id, company);
        return false;
    }

    public int questionnaireBulkDelete(final String company, final List<String> ids) {
        int deletedCount = 0;
        for (String id: ids.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList())) {
            try {
                questionnaireDelete(company, id);
                ++deletedCount;
            } catch (SQLException e) {
                log.error("Failed to delete questionnaire id for company: {}", company);
            }
        }
        return deletedCount;
    }

    public List<Questionnaire> listQuestionnairesByWorkItemId(
            final String company,
            final String workItemId) throws SQLException {
        return questionnaireDBService.listByWorkItemId(company, workItemId, 0, 50).getRecords();
    }

    public List<QuestionnaireDTO> listQuestionnaireDetailsByWorkItemId(
            final String company,
            final String workItemId) throws SQLException {
        List<QuestionnaireDetails> questionnaireDetails = questionnaireDBService.listQuestionnaireDetailsByFilters(company, 0, 50, null, null, List.of(UUID.fromString(workItemId)),
                null, null, null, null, null, null, null, null, null, null, null).getRecords();
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(questionnaireDetails)) {
            return Collections.emptyList();
        }
        return questionnaireDetails.stream().map(x -> {
            try {
                return getQuestionnaireDetails(x);
            } catch (JsonProcessingException e) {
                log.error("Error fetching questionnaire details!", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ImmutablePair<String, String> buildTemplatedTextAndBotName(final String company, String requestorName, QuestionnaireDTO questionnaireDetail) throws SQLException, NotFoundException {
        DbListResponse<MessageTemplate> dbListResponse = msgTemplateService.listByFilter(company, null, null, 0, 100,
                List.of(TemplateType.SLACK), true, List.of(EventType.ASSESSMENT_NOTIFIED));
        if ((dbListResponse == null) || (org.apache.commons.collections4.CollectionUtils.isEmpty(dbListResponse.getRecords()))) {
            String errorMessage = String.format("Could not find default message template for company {}, templateType {} and eventType {}", company, TemplateType.SLACK, EventType.ASSESSMENT_NOTIFIED);
            throw new NotFoundException(errorMessage);
        }
        MessageTemplate notificationTemplate = IterableUtils.first(dbListResponse.getRecords());

        WorkItem workitem = workItemDBService.get(company, questionnaireDetail.getWorkItemId()).orElseThrow(() -> new NotFoundException(String.format("No work item found for company {} and workItemId {}", company, questionnaireDetail.getWorkItemId())));

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("title", workitem.getTitle());
        values.put("link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, workitem.getVanityId()));
        values.put("issue_id", workitem.getVanityId());
        values.put("issue_link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, workitem.getVanityId()));
        values.put("issue_title", workitem.getTitle());
        values.put("assessment_id", questionnaireDetail.getId());
        values.put("assessment_link", String.format(ASSESSMENT_LINK_FORMAT, appBaseUrl, questionnaireDetail.getId(), company));
        values.put("assessment_title", questionnaireDetail.getQuestionnaireTemplateName());
        values.put("sender", MoreObjects.firstNonNull(requestorName, questionnaireDetail.getSenderEmail()));

        String bot = templateService.evaluateTemplate(notificationTemplate.getBotName(), values);
        log.debug("bot {}", bot);
        String content = templateService.evaluateTemplate(notificationTemplate.getMessage(), values);
        log.debug("content {}", content);
        String textWithHeader = SLACK_LEVELOPS_BRAND_NAME_HEADER + content;
        log.debug("textWithHeader {}", textWithHeader);
        return ImmutablePair.of(textWithHeader, bot);
    }

    public String queueQuestionnaireNotification(final String company, final QuestionnaireNotificationRequest questionnaireNotificationRequest) throws SQLException, NotFoundException, IOException {
        UUID questionnaireId = questionnaireNotificationRequest.getQuestionnaireId();

        //Fetch Questionnaire
        QuestionnaireDTO questionnaireDetail = questionnaireDetails(company, questionnaireId);

        ImmutablePair<String, String> templatedData = null;
        try {
            templatedData = buildTemplatedTextAndBotName(company, questionnaireNotificationRequest.getRequestorName(), questionnaireDetail);
        } catch (SQLException | NotFoundException e) {
            log.error("Error building templated text and bot name for questionnaire", e);
            String botName = "LevelOps";
            String questionnaireLink = LevelOpsLinkUtils.buildQuestionnaireLink(appBaseUrl, questionnaireId.toString(), company);
            String templatedText = String.format("Assessment <%s|%s>", questionnaireLink, questionnaireDetail.getQuestionnaireTemplateName());
            templatedData = ImmutablePair.of(templatedText, botName);
        }
        log.debug("templatedData = {}", templatedData);
        return questionnairesNotificationService.queueQuestionnaireNotification(company, questionnaireNotificationRequest, templatedData.getLeft(), templatedData.getRight(), questionnaireDetail);
    }

    public void rebuildQuestionnaireSlackMessageCache(final String company, String questionnaireId) throws SQLException, NotFoundException, IOException {
        //Fetch Questionnaire
        log.debug("Starting rebuilding questionnaire slack cache for company {}, questionnaireId {}!", company, questionnaireId);
        QuestionnaireDTO questionnaireDetail = questionnaireDetails(company, UUID.fromString(questionnaireId));
        questionnairesNotificationService.rebuildQuestionnaireSlackMessageCache(company, questionnaireId, questionnaireDetail);
        log.debug("Successfully rebuilt questionnaire slack cache for company {}, questionnaireId {}!", company, questionnaireId);
    }

    @Async("asyncLowerPriorityTaskExecutor")
    public void rebuildQuestionnaireSlackMessageCacheAsync(final String company, final String questionnaireId) {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notBlank(questionnaireId, "questionnaireId cannot be null or empty!!");
        try {
            rebuildQuestionnaireSlackMessageCache(company, questionnaireId);
        } catch (IOException | SQLException | NotFoundException e) {
            log.error("Error rebuilding questionnaire slack cache for company {}, questionnaireId {}!", company, questionnaireId);
        }
    }


    @SuppressWarnings("unchecked")
    public PaginatedResponse<QuestionnaireListItemDTO> questionnaireList(final String company,
                                                                         final DefaultListRequest filter,
                                                                         final String targetEmail) throws SQLException {
        // TODO convert DefaultListRequest.getFilter() to QuestionnaireFilter
        // (remove discrepancies in field names)

        String priority = (String) filter.getFilter().get("priority");
        List<String> questionnaireTemplateIds = filter.getFilterValue(
                "questionnaire_template_ids", List.class)
                .orElse(Collections.emptyList());
        List<String> ids = filter.getFilterValue("ids", List.class).orElse(Collections.emptyList());
        List<String> tagIds = filter.getFilterValue("tag_ids", List.class).orElse(Collections.emptyList());
        List<String> assigneeUserIds = filter.getFilterValue("assignee_user_ids", List.class).orElse(Collections.emptyList());
        List<UUID> workItemIds = (List<UUID>) filter.getFilterValue("work_item_ids", List.class)
                .orElse(Collections.emptyList())
                .stream()
                .map(name -> UUID.fromString((String) name))
                .collect(Collectors.toList());
        String productId = (String) filter.getFilter().get("product_id");
        Boolean completed = (Boolean) filter.getFilter().get("completed");
        Questionnaire.State state = Questionnaire.State.fromString((String) filter.getFilter().get("state"));
        Optional<DateFilter> updatedAtFilter = filter.getFilterValue("updated_at", Object.class)
                .map(o -> objectMapper.convertValue(o, DateFilter.class));
        Optional<DateFilter> createdAtFilter = filter.getFilterValue("created_at", Object.class)
                .map(o -> objectMapper.convertValue(o, DateFilter.class));

        Boolean main = filter.getFilterValue("main", Boolean.class).orElse(null);


        DbListResponse<QuestionnaireDetails> results = questionnaireDBService.listQuestionnaireDetailsByFilters(company, filter.getPage(), filter.getPageSize(), QuestionnaireFilter.builder()
                .productId(productId)
                .priority(priority)
                .workItemIds(workItemIds)
                .questionnaireTemplateIds(UUIDUtils.fromStringsList(questionnaireTemplateIds))
                .ids(UUIDUtils.fromStringsList(ids))
                .tagIds(tagIds)
                .targetEmail(targetEmail)
                .main(main)
                .state(state)
                .isFullyAnswered(completed)
                .updatedAt(updatedAtFilter.orElse(null))
                .createdAt(createdAtFilter.orElse(null))
                .assigneeUserIds(assigneeUserIds)
                .build());
        if (results == null || results.getCount() < 1) {
            return PaginatedResponse.of(filter.getPage(),
                    filter.getPageSize(), 0, Collections.emptyList());
        }
        return PaginatedResponse.of(
                filter.getPage(),
                filter.getPageSize(),
                results.getTotalCount(),
                results.getRecords()
                        .stream()
                        .map(item -> {
                            List<SectionResponse> answers = List.of();
                            List<Section> sections = List.of();
                            try {
                                QuestionnaireDTO questionnaireDetails = getQuestionnaireDetails(item);
                                answers = questionnaireDetails.getAnswers();
                                sections = questionnaireDetails.getSections();
                            } catch (JsonProcessingException e) {
                                log.warn("Error processing questionnaire details for company {}, questionnaireId {}!", company, item.getId());
                            }
                            return QuestionnaireListItemDTO.builder()
                                    .id(item.getId())
                                    .workItemId(item.getWorkItemId())
                                    .reason(item.getReason())
                                    .questionnaireTemplateId(item.getQuestionnaireTemplateId())
                                    .questionnaireTemplateName(item.getQuestionnaireTemplateName())
                                    .currentScore(item.getScore())
                                    .priority(item.getPriority())
                                    .productId(item.getProductId())
                                    .totalQuestions(item.getTotalQuestions())
                                    .targetEmail(item.getTargetEmail())
                                    .answeredQuestions(item.getAnswered())
                                    .updatedAt(item.getUpdatedAt())
                                    .createdAt(item.getCreatedAt())
                                    .completed(item.getState() == COMPLETED)
                                    .tagIds(item.getTagIds())
                                    .state(item.getState())
                                    .main(item.getMain())
                                    .sections(sections)
                                    .answers(answers)
                                    .build();
                        })
                        .collect(Collectors.toList())
        );
    }
    // endregion

    public PaginatedResponse<DbAggregationResult> stackedAggregate(String company, QuestionnaireAggFilter filter,
                                                                   List<QuestionnaireAggFilter.Distinct> stacks,
                                                                   DefaultListRequest request) throws SQLException {
        var results = questionnaireDBService
                .stackedAggregate(company, filter, stacks, 0, 90);
        if (results == null || results.getCount() < 1) {
            return PaginatedResponse.of(request.getPage(),
                    request.getPageSize(), 0, Collections.emptyList());
        }
        return PaginatedResponse.of(0, 90,
                results.getTotalCount(),
                results.getRecords()
        );
    }

    public PaginatedResponse<DbAggregationResult> stackedAggregateResponseTime(String company, QuestionnaireAggFilter filter,
                                                                               List<QuestionnaireAggFilter.Distinct> stacks,
                                                                               DefaultListRequest request) throws SQLException {
        var results = questionnaireDBService
                .stackedAggregate(company, filter, stacks, request.getPage(), request.getPageSize());
        if (results == null || results.getCount() < 1) {
            return PaginatedResponse.of(request.getPage(),
                    request.getPageSize(), results.getTotalCount(), Collections.emptyList());
        }
        return PaginatedResponse.of(request.getPage(), request.getPageSize(),
                results.getTotalCount(),
                results.getRecords()
        );
    }
}
