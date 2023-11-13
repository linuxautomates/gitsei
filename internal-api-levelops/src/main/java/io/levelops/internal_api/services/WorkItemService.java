package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.Notification;
import io.levelops.commons.databases.models.database.NotificationRequestorType;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItem.TicketType;
import io.levelops.commons.databases.models.database.WorkItemNotification;
import io.levelops.commons.databases.models.database.WorkItemNotificationRequest;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequest;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequestWithText;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.databases.services.QuestionnaireTemplateDBService;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.databases.services.WorkItemDBService;
import io.levelops.commons.databases.services.WorkItemNotificationsDatabaseService;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.files.services.FileStorageService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.internal_api.services.utils.ServiceUtils;
import io.levelops.notification.models.SlackNotificationResult;
import io.levelops.notification.services.NotificationService;
import io.levelops.uploads.services.FilesService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Log4j2
@Service
public class WorkItemService {
    private static final String ASSESSMENT_LINK_FORMAT = "%s/#/admin/answer-questionnaire-page?questionnaire=%s&tenant=%s";
    public static final String WORKITEM_LINK_FORMAT = "%s/#/admin/workitems/details?workitem=%s";
    private final static Pattern VANITY_ID_PATTERN = Pattern.compile("^(?<key>.*)-(?<seq>.*)$");
    private static final boolean QUESTIONNAIRE_IS_MAIN = true;

    private final ForkJoinPool notificationForkJoinPool;
    private final WorkItemDBService workItemDBService;
    private final TagItemService tagItemService;
    private final QuestionnaireTemplateDBService questionnaireTemplateDBService;
    private final QuestionnaireService questionnaireService;
    private final TicketTemplateDBService ticketTemplateDBService;
    private final StateDBService stateDBService;
    private final NotificationService notificationService;
    private final MsgTemplateService msgTemplateService;
    private final FilesService filesService;
    private final WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService;
    private final WorkItemsNotificationService workItemsNotificationService;
    private final String appBaseUrl;
    private final EventsClient eventsClient;
    private final ObjectMapper mapper;
    private final FileStorageService fileStorageService;
    private final ControlPlaneService controlPlaneService;
    private final long maxWaitTimeForNotification;

    @Autowired
    public WorkItemService(WorkItemDBService workItemDBService, TagItemService tagItemService,
                           QuestionnaireTemplateDBService questionnaireTemplateDBService, QuestionnaireService questionnaireService,
                           TicketTemplateDBService ticketTemplateDBService, StateDBService stateDBService,
                           NotificationService notificationService, final MsgTemplateService msgTemplateService,
                           @Value("${APP_BASE_URL:https://app.propelo.ai}") final String appBaseUrl,
                           final FilesService filesService,
                           @Value("${work_items.notification_threads:4}") int notificationThreads, WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService, WorkItemsNotificationService workItemsNotificationService, EventsClient eventsClient, ObjectMapper mapper, FileStorageService fileStorageService, ControlPlaneService controlPlaneService,
                           @Value("${work_items.notification_max_wait_time_mins:5}") int maxWaitTimeForNotificationInMinutes) {
        this.workItemDBService = workItemDBService;
        this.tagItemService = tagItemService;
        this.questionnaireTemplateDBService = questionnaireTemplateDBService;
        this.questionnaireService = questionnaireService;
        this.ticketTemplateDBService = ticketTemplateDBService;
        this.stateDBService = stateDBService;
        this.notificationService = notificationService;
        this.appBaseUrl = appBaseUrl;
        this.msgTemplateService = msgTemplateService;
        this.filesService = filesService;
        this.notificationForkJoinPool = new ForkJoinPool(notificationThreads);
        this.workItemNotificationsDatabaseService = workItemNotificationsDatabaseService;
        this.workItemsNotificationService = workItemsNotificationService;
        this.eventsClient = eventsClient;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.controlPlaneService = controlPlaneService;
        this.maxWaitTimeForNotification = TimeUnit.MINUTES.toSeconds(maxWaitTimeForNotificationInMinutes);
    }

    private List<QuestionnaireDTO> createQuestionnaires(final String company, final String workItemId,
                                                        final WorkItem workItem) throws SQLException {
        String ticketTemplateId = workItem.getTicketTemplateId();
        if (StringUtils.isBlank(ticketTemplateId)) {
            return List.of();
        }
        TicketTemplate ticketTemplate = ticketTemplateDBService.get(company, ticketTemplateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ticket template does not exist : ticket_template_id : " + ticketTemplateId));
        AtomicBoolean invalidQTemplate = new AtomicBoolean(false);
        List<QuestionnaireTemplate> qtemplateList = ticketTemplate.getMappings().stream().map(mapping -> {
            String questionnaireTemplateId = mapping.getQuestionnaireTemplateId();
            try {
                return questionnaireTemplateDBService.get(company, questionnaireTemplateId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Questionnaire Template used in Ticket template does not exist : ticket_template_id : "
                                        + ticketTemplateId + " questionnaire template id : "
                                        + questionnaireTemplateId));
            } catch (SQLException | ResponseStatusException e) {
                invalidQTemplate.set(true);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (invalidQTemplate.get()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One of the Questionnaire Templates used in Ticket template does not exist : ticket_template_id : "
                            + ticketTemplateId);
        }

        AtomicBoolean errorCreatingQuestionnaire = new AtomicBoolean(false);
        List<QuestionnaireDTO> assessments = new ArrayList<>();
        qtemplateList.forEach(qt -> {
            log.debug("Working on qt {}", qt);
            QuestionnaireDTO q = QuestionnaireDTO.builder().workItemId(workItemId)
                    // main questionnaires are created and answered by the same person
                    .senderEmail(workItem.getReporter()).targetEmail(workItem.getReporter())
                    .questionnaireTemplateId(qt.getId()).productId(workItem.getProductId()).main(QUESTIONNAIRE_IS_MAIN)
                    .kbIds((CollectionUtils.isNotEmpty(qt.getKbIds()) ? qt.getKbIds() : Collections.emptyList()))
                    .build();
            log.debug("Starting to create questionnaire {}", q);
            try {
                var qId = questionnaireService.createQuestionnaire(company, q);
                assessments.add(q.toBuilder().id(qId).build());
            } catch (SQLException | JsonProcessingException | BadRequestException e) {
                log.error("Error creating Questionnaire {}",q, e);
                errorCreatingQuestionnaire.set(true);
            }
        });
        if (errorCreatingQuestionnaire.get()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error Creating Questionnaire. Qtemplate used in Ticket template does not exist: ticket_template : "
                            + ticketTemplateId);
        }
        return assessments;
    }

    public void createWorkItemWithoutQuestionnaire(final String company, final WorkItem workitem) throws SQLException {
        workItemDBService.insert(company, workitem);
    }

    private WorkItem createWorkItemInternal(final String company, final WorkItem workitem) throws SQLException {
        String id = workItemDBService.insert(company, workitem);
        List<QuestionnaireDTO> assessments = createQuestionnaires(company, id, workitem);
        tagItemService.batchInsert(company, UUID.fromString(id), WorkItem.ITEM_TYPE, workitem.getTagIds());

        var finalWorkItem = workItemDBService.get(company, id).orElseThrow(() ->
                new SQLException("Could not get newly created Work Item id = " + id));
        if (!Boolean.TRUE.equals(workitem.getNotify())) {
            return finalWorkItem;
        }
        var optionalTemplate = ticketTemplateDBService.get(company, workitem.getTicketTemplateId());
        if (optionalTemplate.isPresent() && optionalTemplate.get().getNotifyBy().size() > 0) {
            log.info("Sending notifications for ticket template id {}", workitem.getTicketTemplateId());

            notificationForkJoinPool.submit(() -> {
                Set<String> sent = new HashSet<>();
                workitem.getAssignees().forEach(assignee -> {
                    sent.add(assignee.getUserEmail().trim().toLowerCase());
                    notify(company, finalWorkItem, optionalTemplate.get(), EventType.SMART_TICKET_CREATED, assignee.getUserEmail(), assessments);
                });
                if (!sent.contains(workitem.getReporter().trim().toLowerCase())) {
                    notify(company, finalWorkItem, optionalTemplate.get(), EventType.SMART_TICKET_CREATED, workitem.getReporter(), assessments);
                }
            });
        } else {
            log.info("Not sending notifications since no ticket template was fount for the template id {}", workitem.getTicketTemplateId());
        }
        return finalWorkItem;
    }
    public WorkItem createWorkItem(final String company, final WorkItem workitem) throws SQLException {
        WorkItem createdWi = createWorkItemInternal(company, workitem);
        sendTicketCreatedEvent(company, createdWi);
        return createdWi;
    }

    private void sendTicketCreatedEvent(final String company, WorkItem createdWi){
        sendTicketCreatedEvent(company, createdWi, null, null);
    }

    private void sendTicketCreatedEvent(final String company, WorkItem createdWi, Boolean escalate,
                                        List<String> notificationUrls) {
        try {
            eventsClient.emitEvent(company, EventType.SMART_TICKET_CREATED, Map.of(
                    "id", createdWi.getId(),
                    "ticket_type", createdWi.getTicketType(),
                    "products", createdWi.getProductId() != null ? createdWi.getProductId() : "",
                    "tags", createdWi.getTagIds() != null ? createdWi.getTagIds() : List.of(),
                    "integration", createdWi.getIntegrationId() != null ? createdWi.getIntegrationId() : "",
                    "template", createdWi.getTicketTemplateId() != null ? createdWi.getTicketTemplateId() : "",
                    "escalate", Boolean.TRUE.equals(escalate),
                    "notification_urls", CollectionUtils.emptyIfNull(notificationUrls)
            ));
        } catch (EventsClientException e) {
            log.error("Failed to emit the event for the creation of a new ticket/workItem. ticket id={}", createdWi.getId(), e);
        }
    }

    private WorkItem createSnippetWorkItemInternal(final String company, final CreateSnippetWorkitemRequest request, final String snippetName, final String snippetFileName, final String snippetContentType, final String snippetText) throws IOException, SQLException {
        log.info("request {}", request);
        //Create WI from CreateSnippetWorkitemRequest
        WorkItem workItem = WorkItem.builder()
                .type(WorkItem.ItemType.MANUAL).ticketType(TicketType.SNIPPET)
                .title(request.getTitle())
                .reporter(request.getRequestor())
                .reason(request.getUrl())
                .description(request.getMessage())
                .build();
        log.info("workItem {}", workItem);

        //Persist WI to DB
        WorkItem createdWi = createWorkItemInternal(company, workItem);
        log.info("createdWi {}", createdWi);

        //Upload snippet file & get snippet upload id
        String snippetUploadId = fileStorageService.uploadNewFileForComponent(company, request.getRequestor(), ComponentType.SMART_TICKET.getStorageName(), createdWi.getId(),
                snippetName, snippetFileName, snippetContentType, snippetText.getBytes(UTF_8));
        log.info("snippetUploadId {}", snippetUploadId);

        //Modify WI & add uploaded snippet file as attachment
        WorkItem updatedWi = createdWi.toBuilder().attachment(WorkItem.Attachment.builder().uploadId(snippetUploadId).fileName(snippetFileName).build()).build();
        log.info("updatedWi {}", updatedWi);

        //Persist modified wi with snippet file as attachment
        var updateStatus = workItemDBService.update(company, updatedWi);
        log.info("updateStatus {}", updateStatus);
        if(!updateStatus) {
            throw new RuntimeException("Workitem not found, id " + createdWi.getId());
        }

        //Send WI notification (this is mainly for recipients who are not assignees and reporters)
        List<String> notificationUrls = null;
        if(CollectionUtils.isNotEmpty(request.getRecipients())) {
            WorkItemNotificationRequest workItemNotificationRequest = WorkItemNotificationRequest.builder()
                    .workItemId(UUID.fromString(updatedWi.getId()))
                    .recipients(request.getRecipients())
                    .mode(request.getMode())
                    .requestorType(NotificationRequestorType.USER)
                    .requestorName(request.getRequestor())
                    .message(request.getMessage())
                    .build();
            log.info("workItemNotificationRequest {}", workItemNotificationRequest);
            final boolean sendSync = Boolean.TRUE.equals(request.getEscalate());
            final SlackNotificationResult result = workItemsNotificationService.queueWorkItemNotification(company,
                    workItemNotificationRequest, updatedWi, Map.of(UUID.fromString(snippetUploadId), snippetText),
                    sendSync);
            if (sendSync) {
                final SlackChatInteractiveMessageResult messageResult = result.getMessageResult();
                if (messageResult != null) {
                    notificationUrls = CollectionUtils.emptyIfNull(messageResult.getNotifications())
                            .stream()
                            .map(Notification::getUrl)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }
        }

        //Last step is to send ticket created event
        sendTicketCreatedEvent(company, updatedWi, request.getEscalate(), notificationUrls);
        return updatedWi;
    }

    public WorkItem createSnippetWorkItemMultipart(final String company, final MultipartFile createSnippetWorkItemRequest, final MultipartFile snippetFile) throws IOException, SQLException {
        CreateSnippetWorkitemRequest request = mapper.readValue(createSnippetWorkItemRequest.getBytes(), CreateSnippetWorkitemRequest.class);
        log.info("request {}", request);
        return createSnippetWorkItemInternal(company, request, snippetFile.getName(), snippetFile.getOriginalFilename(), snippetFile.getContentType(), new String(snippetFile.getBytes(), UTF_8));
    }

    public WorkItem createSnippetWorkItem(final String company, final CreateSnippetWorkitemRequestWithText createSnippetWorkitemRequest) throws IOException, SQLException {
        log.debug("createSnippetWorkitemRequest = {}", createSnippetWorkitemRequest);
        CreateSnippetWorkitemRequest request = mapper.readValue(mapper.writeValueAsString(createSnippetWorkitemRequest), CreateSnippetWorkitemRequest.class);
        log.debug("request {}", request);
        return createSnippetWorkItemInternal(company, request, "snippet.txt", "snippet.txt", "text/plain", createSnippetWorkitemRequest.getSnippet());
    }

    private void notify(String company, WorkItem workitem, TicketTemplate ticketTemplate, EventType eventType,
                        String recipientEmail, List<QuestionnaireDTO> assessments) {
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
            log.warn("No template found for the ticker '{}'", workitem.getId());
            return;
        }
        var values = new HashMap<String, Object>();
        values.put("title", workitem.getTitle());
        values.put("link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, workitem.getVanityId()));
        values.put("issue_id", workitem.getVanityId());
        values.put("issue_link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, workitem.getVanityId()));
        values.put("issue_title", workitem.getTitle());
        if (CollectionUtils.isNotEmpty(assessments)) {
            assessments.forEach(assessment -> {
                // TODO: allow more than one
                values.put("assessment_id", assessment.getId());
                values.put("assessment_link", String.format(ASSESSMENT_LINK_FORMAT, appBaseUrl, assessment.getId(), company));
            });
        }
        notificationTemplates.forEach(notificationTemplate -> {
            try {
                notificationService.sendNotification(company, notificationTemplate, recipientEmail, values);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    log.error("Unable to send a notification to {} for {}...", recipientEmail, eventType, e);
                } else {
                    log.error("Unable to send a notification to {} for {}... {}", recipientEmail, eventType, e);
                }
            }
        });
    }

    private Optional<WorkItem> mergeWorkItemWithNotifications(final String company, Optional<WorkItem> optionalWorkItem) throws SQLException {
        if(optionalWorkItem.isEmpty()) {
            return optionalWorkItem;
        }
        WorkItem workItem = optionalWorkItem.get();
        DbListResponse<WorkItemNotification> notificationDbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 100,
                null, List.of(UUID.fromString(workItem.getId())), null, null, null);

        if(CollectionUtils.isNotEmpty(notificationDbListResponse.getRecords())) {
            List<WorkItem.Notification> notifications = notificationDbListResponse.getRecords().stream()
                    .map(x -> WorkItem.Notification.builder().mode(x.getMode()).recipient(x.getRecipient()).url(x.getUrl()).createdAt(x.getCreatedAt()).build()).collect(Collectors.toList());

            workItem = workItem.toBuilder().notifications(List.of(Iterables.getLast(notifications))).build();
        }
        return Optional.ofNullable(workItem);
    }

    public Optional<WorkItem> getWorkItem(final String company, final UUID id) throws SQLException {
        Optional<WorkItem> optionalWorkItem = workItemDBService.get(company, id.toString());
        return mergeWorkItemWithNotifications(company, optionalWorkItem);
    }

    public Optional<WorkItem> getWorkItemByVanityId(final String company, final String vanityId) throws Exception {
        Matcher matcher = VANITY_ID_PATTERN.matcher(vanityId);
        if (!matcher.matches()) {
            throw new Exception("Vanity id is invalid: " + vanityId);
        }
        String key = matcher.group("key");
        Long seqNumber = Long.parseLong(matcher.group("seq"));
        Optional<WorkItem> optionalWorkItem = workItemDBService.getByVanitySequenceNumber(company, key, seqNumber);
        return mergeWorkItemWithNotifications(company, optionalWorkItem);
    }

    //Delete once clients are upgraded
    private WorkItem sanitizeStateId(final String company, final WorkItem workItem) throws SQLException {
        if (StringUtils.isEmpty(workItem.getStatus())) {
            return null;
        }
        State state = stateDBService.getStateByName(company, workItem.getStatus());
        return workItem.toBuilder().stateId(state.getId()).build();
    }

    public Boolean updateWorkItem(final String company, final UUID id, final WorkItem workItem) throws SQLException {
        final WorkItem sanitized = sanitizeStateId(company, workItem.toBuilder().id(id.toString()).build());
        if (CollectionUtils.isNotEmpty(sanitized.getTagIds())) { // for testing only...
            // delete previous tag associations.
            tagItemService.deleteTagsForItem(company, id, WorkItem.ITEM_TYPE);
            tagItemService.batchInsert(company, id, WorkItem.ITEM_TYPE, sanitized.getTagIds());
        }
        final Optional<WorkItem> original = workItemDBService.get(company, id.toString());
        var status = workItemDBService.update(company, sanitized);
        if (!Boolean.TRUE.equals(workItem.getNotify())) {
            log.warn("Not sending notification since the notify flag is off. workItem={}", id);
            return status;
        }

        notificationForkJoinPool.submit(() -> {
            try {
                if (original.isEmpty()) {
                    log.warn("Not sending notification since the original workItem was not found and thus we cannot diff the update... workItem={}", id);
                    return;
                }
                var optionalTemplate = ticketTemplateDBService.get(company, original.get().getTicketTemplateId());
                if (optionalTemplate.isEmpty() || optionalTemplate.get().getNotifyBy().size() <= 0) {
                    log.info("Not sending notifications since no ticket template was fount for the template id {}", original.get().getTicketTemplateId());
                    return;
                }
                var ids = optionalTemplate.get().getMappings().stream()
                        .map(item -> item.getQuestionnaireTemplateId().toLowerCase())
                        .filter(Predicate.not(String::isBlank))
                        .collect(Collectors.toList());
                List<QuestionnaireDTO> assessments = questionnaireService.listQuestionnairesByWorkItemId(company, id.toString()).stream()
                        .filter(item -> ids.contains(item.getQuestionnaireTemplateId()))
                        .map(item -> QuestionnaireDTO.builder().id(item.getId()).build())
                        .collect(Collectors.toList());
                List<String> assignees = original.get().getAssignees().stream()
                        .map(assignee -> assignee.getUserEmail().toLowerCase().trim())
                        .collect(Collectors.toList());
                log.info("Sending notifications...");
                sanitized.getAssignees().stream()
                        .map(assignee -> assignee.getUserEmail().toLowerCase().trim())
                        .filter(assignee -> !assignees.contains(assignee.trim().toLowerCase()))
                        .forEach(assignee ->
                                notify(company, original.get(), optionalTemplate.get(), EventType.SMART_TICKET_NEW_ASSIGNEE, assignee, assessments));
            } catch (SQLException e) {
                log.error("Unable to send notification...", e);
            }
        });
        return status;
    }

    public void changeProductId(final String company, final UUID workitemId, final String newProductId) throws SQLException {
        workItemDBService.updateProductId(company, workitemId, newProductId);
        workItemDBService.get(company, workitemId.toString()).orElseThrow(() -> new SQLException("Could not get updated Work Item id = " + workitemId));
    }

    public void changeParentId(final String company, final UUID workitemId, final UUID newParentId) throws SQLException {
        workItemDBService.updateParentId(company, workitemId, newParentId);
    }

    public void changeState(final String company, final UUID workitemId, final String newState) throws SQLException {
        State state = stateDBService.getStateByName(company,newState);
        workItemDBService.updateState(company, workitemId.toString(), state.getId());
    }

    /**
     * Deletes a workitem, assessments associated with it and attachments associated
     * to both, workitem and assessments.
     *
     * @param company tenant
     * @param id      the workitem id
     * @return true if the workitem and all associated resources are successfully
     * deleted
     * @throws SQLException                  if there is any issue with the db
     *                                       communication.
     * @throws NotFoundException             if the workitem is not present in the
     *                                       tanant's db.
     * @throws UnsupportedOperationException if the workitem is of the type
     *                                       'automated'.
     * @throws IOException
     */
    public Boolean deleteWorkItem(final String company, final String user, final RoleType role, final UUID id)
            throws SQLException, NotFoundException, UnsupportedOperationException, IOException {
        Optional<WorkItem> optionalItem = workItemDBService.get(company, id.toString());
        if (optionalItem.isEmpty()) {
            log.debug("deleteWorkItem : Company {}, Work Item with Id {} is not found. Hence ignoring the delete request", company, id);
            return false;
        }
        // delete attachments
        filesService.deleteEverythingUnderComponent(
                company,
                user,
                role,
                optionalItem.get().getTicketType() == TicketType.WORK_ITEM ? ComponentType.WORK_ITEM : ComponentType.SMART_TICKET,
                id.toString());
        // delete assessments
        List<Questionnaire> assessments = questionnaireService.listQuestionnairesByWorkItemId(company, id.toString());
        if (CollectionUtils.isNotEmpty(assessments)) {
            for (Questionnaire assessment : assessments) {
                questionnaireService.questionnaireDelete(company, assessment.getId());
            }
        }
        // delete tags
        tagItemService.deleteTagsForItem(company, id, WorkItem.ITEM_TYPE);
        // delete work item
        return workItemDBService.delete(company, id.toString());
    }

    public List<DeleteResponse> bulkDeleteWorkItems(final String company, final String user, final RoleType role, final List<UUID> ids) throws IOException, SQLException, NotFoundException {
        return ids.stream()
                .map(id -> {
                    try {
                        if (!deleteWorkItem(company, user, role, id)) {
                            log.debug("bulkDeleteWorkItems : Company {}, Work Item with Id {} is not found and hence not deleted", company, id);
                        }
                        return DeleteResponse.builder()
                                .id(id.toString())
                                .success(true)
                                .build();
                    } catch (Exception e) {
                        return DeleteResponse.builder()
                                .id(id.toString())
                                .success(false)
                                .error(e.getMessage())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<WorkItem> listWorkItems(final String company, final DefaultListRequest filter) throws SQLException {
        log.debug("filter = {}", filter);

        String artifact = filter.getFilterValue("partial", Map.class)
                .map(map -> (String) map.get("artifact"))
                .orElse(null);
        String artifactTitle = filter.getFilterValue("partial", Map.class)
                .map(map -> (String) map.get("artifact_title"))
                .orElse(null);
        Integer createdAfter = filter.getFilterValue("created_after", Integer.class)
                .orElse(null);
        String reporter = filter.getFilterValue("reporter", String.class)
                .orElse(null);
        Set<String> reporters = filter.<String>getFilterValueAsSet("reporters").orElse(Set.of());
        Map<String, Integer> sorting = filter.getSort().stream().collect(Collectors.toMap(
                item -> (String) item.get("id"),
                item -> Boolean.TRUE.equals(item.get("desc")) ? -1 : 1
        ));

        Map<String, Integer> createRange = filter.getFilterValue("created_at", Map.class)
                .orElse(Map.of());
        Long createdAtStart = createRange.get("$gt") != null ? Long.valueOf(createRange.get("$gt")) : null;
        Long createdAtEnd = createRange.get("$lt") != null ? Long.valueOf(createRange.get("$lt")) : null;

        Map<String, Integer> updateRange = filter.getFilterValue("updated_at", Map.class)
                .orElse(Map.of());
        Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;
        Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;

        List<String> idsString = filter.getFilterValue("ids", List.class).orElse(Collections.emptyList());
        List<UUID> ids = idsString.stream().map(UUID::fromString).collect(Collectors.toList());

        List<String> assigneeUserIdsString = filter.getFilterValue("assignee_user_ids", List.class).orElse(Collections.emptyList());
        List<Integer> assigneeUserIds = assigneeUserIdsString.stream()
                .map(item -> NumberUtils.toInt(item, -1))
                .filter(item -> item > 0)
                .collect(Collectors.toList());

        List<String> questionnaireTemplateIdsString = filter.getFilterValue("questionnaire_template_ids", List.class).orElse(Collections.emptyList());
        List<UUID> questionnaireTemplateIds = questionnaireTemplateIdsString.stream().map(UUID::fromString).collect(Collectors.toList());

        List<String> productIdsString = filter.getFilterValue("product_ids", List.class).orElse(Collections.emptyList());
        List<Integer> productIds = productIdsString.stream().map(Integer::parseInt).collect(Collectors.toList());

        String title = filter.getFilterValue("partial", Map.class).map(map -> (String) map.get("title")).orElse(null);

        Boolean unassigned = filter.getFilterValue("unassigned", Boolean.class).orElse(null);

        List<String> tagIds = filter.getFilterValue("tag_ids", List.class).orElse(Collections.emptyList());

        String type = filter.getFilterValue("type", String.class).orElse(null);

        String status = filter.getFilterValue("status", String.class).orElse(null);

        Severity priority = Severity.fromString(filter.getFilterValue("priority", String.class).orElse(null));

        List<String> cicdJobRunIdsString = filter.getFilterValue("cicd_job_run_ids", List.class).orElse(Collections.emptyList());
        List<UUID> cicdJobRunIds = cicdJobRunIdsString.stream().map(UUID::fromString).collect(Collectors.toList());

        List<String> cicdJobRunStageIdsString = filter.getFilterValue("cicd_job_run_stage_ids", List.class).orElse(Collections.emptyList());
        List<UUID> cicdJobRunStageIds = cicdJobRunStageIdsString.stream().map(UUID::fromString).collect(Collectors.toList());


        return workItemDBService.listByFilter(
                company,
                filter.getPage(),
                filter.getPageSize(),
                ids,
                title,
                BooleanUtils.isTrue(unassigned),
                assigneeUserIds,
                questionnaireTemplateIds,
                tagIds,
                reporter,
                reporters,
                productIds,
                type,
                status,
                priority,
                artifact,
                artifactTitle,
                cicdJobRunIds, cicdJobRunStageIds,
                createdAtStart,
                createdAtEnd,
                updatedAtStart,
                updatedAtEnd,
                createdAfter,
                sorting.get("updated_at"),
                sorting.get("priority"),
                sorting.get("due_at"),
                sorting.get("created_at"));
    }

    public SlackNotificationResult queueNotification(final String company, final WorkItemNotificationRequest workItemNotificationRequest) throws SQLException, NotFoundException, IOException {
        UUID workItemId = workItemNotificationRequest.getWorkItemId();
        //Fetch WI
        WorkItem workItem = getWorkItem(company, workItemId).orElseThrow(() -> new NotFoundException("Could not find work item with id=" + workItemId.toString()));
        Map<UUID, String> uploadIdTextAttachmentsMap = new HashMap<>();
        if(TicketType.SNIPPET.equals(workItem.getTicketType())) {
            if(CollectionUtils.isNotEmpty(workItem.getAttachments())) {
                String uploadId = workItem.getAttachments().get(0).getUploadId();
                byte[] snippetsBytes = fileStorageService.downloadFileForComponent(company, ComponentType.SMART_TICKET.getStorageName(), workItemId.toString(), uploadId);
                String snippetsText = new String(snippetsBytes, UTF_8);
                uploadIdTextAttachmentsMap.put(UUID.fromString(uploadId), snippetsText);
            }
        }
        return workItemsNotificationService.queueWorkItemNotification(company, workItemNotificationRequest, workItem, uploadIdTextAttachmentsMap, false);
    }

    public void rebuildWorkItemViewTextAttachmentSlackCache(final String company, final String workItemId, final String uploadId) throws SQLException, NotFoundException, IOException {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notBlank(workItemId, "workItemId cannot be null or empty!!");
        Validate.notBlank(uploadId, "uploadId cannot be null or empty!!");

        //Fetch WI
        WorkItem workItem = getWorkItem(company, UUID.fromString(workItemId)).orElseThrow(() -> new NotFoundException("Could not find work item with id=" + workItemId));

        //Fetch Snippet Text
        byte[] snippetsBytes = fileStorageService.downloadFileForComponent(company, ComponentType.SMART_TICKET.getStorageName(), workItemId, uploadId);
        String snippetsText = new String(snippetsBytes, UTF_8);
        Map<UUID, String> uploadIdTextAttachmentsMap = Map.of(UUID.fromString(uploadId), snippetsText);

        //Rebuild WI View Text Attachment Cache
        workItemsNotificationService.rebuildWorkItemViewTextAttachmentSlackMessageCache(company, workItem, uploadIdTextAttachmentsMap);
    }

    public DbListResponse<DbAggregationResult> stackedAggregate(String company, WorkItemFilter filter, List<WorkItemFilter.Distinct> stacks) throws SQLException {
        return workItemDBService.stackedAggregate(company, filter, stacks);
    }
}
