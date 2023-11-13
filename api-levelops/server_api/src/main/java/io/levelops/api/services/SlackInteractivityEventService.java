package io.levelops.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.api.model.slack.SlackInteractiveEvent;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.NotificationRequestorType;
import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItemNotification;
import io.levelops.commons.databases.models.database.WorkItemNotificationRequest;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SlackTenantLookupDatabaseService;
import io.levelops.commons.databases.services.SlackUsersDatabaseService;
import io.levelops.commons.databases.services.WorkItemNotificationsDatabaseService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.exceptions.EmailException;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.notification.clients.SlackBotClientFactory;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.notification.models.SlackApiUser;
import io.levelops.notification.services.QuestionnaireSlackMessageUtils;
import io.levelops.notification.services.SlackQuestionnaireCacheService;
import io.levelops.notification.services.SlackWorkItemCacheService;
import io.levelops.notification.services.WorkItemSlackMessageUtils;
import io.levelops.users.requests.ModifyUserRequest;
import io.levelops.workitems.clients.QuestionnaireNotificationClient;
import io.levelops.workitems.clients.WorkItemsClient;
import io.levelops.workitems.clients.WorkItemsNotificationClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
@Service
public class SlackInteractivityEventService {
    public static final String PAYLOAD_EQUALS = "payload=";
    static final Pattern REQUESTOR_MESSAGE_PATTERN = Pattern.compile("^From (.*),\\s*(?<message>.*)$");

    private final String slackVerificationToken;
    private final ObjectMapper objectMapper;
    private final SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;
    private final WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService;
    private final WorkItemsNotificationClient workItemsNotificationClient;
    private final WorkItemsClient workItemsClient;
    private final ActivityLogService activityLogService;
    private final SlackQuestionnaireCacheService slackQuestionnaireCacheService;
    private final SlackBotClientFactory slackBotClientFactory;
    private final InventoryService inventoryService;
    private final SlackSubmitQuestionnaireService slackSubmitQuestionnaireService;
    private final QuestionnaireNotificationClient questionnaireNotificationClient;
    private final SlackWorkItemCacheService slackWorkItemCacheService;
    private final SlackUsersDatabaseService slackUsersDatabaseService;
    private final UserClientHelperService userClientHelperService;
    private final WorkItemService workItemService;

    private final LoadingCache<String, IntegrationKey> SLACK_INTEGRATION_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(0) // DISABLED CACHING FOR NOW
            .build(new CacheLoader<>() {
                @Override
                public IntegrationKey load(@Nonnull String tenantId) throws Exception {
                    return findSlackIntegration(tenantId)
                            .orElseThrow(() -> new Exception("Could not find an active Slack integration for tenant: "
                                    + tenantId));
                }
            });

    @Autowired
    public SlackInteractivityEventService(@Value("${SLACK_VERIFICATION_TOKEN}") String slackVerificationToken, ObjectMapper objectMapper, SlackTenantLookupDatabaseService slackTenantLookupDatabaseService, WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService, WorkItemsNotificationClient workItemsNotificationClient, WorkItemsClient workItemsClient, ActivityLogService activityLogService, SlackQuestionnaireCacheService slackQuestionnaireCacheService, SlackBotClientFactory slackBotClientFactory, InventoryService inventoryService, SlackSubmitQuestionnaireService slackSubmitQuestionnaireService, QuestionnaireNotificationClient questionnaireNotificationClient, SlackWorkItemCacheService slackWorkItemCacheService, SlackUsersDatabaseService slackUsersDatabaseService, UserClientHelperService userClientHelperService, WorkItemService workItemService) {
        this.slackVerificationToken = slackVerificationToken;
        this.objectMapper = objectMapper;
        this.slackTenantLookupDatabaseService = slackTenantLookupDatabaseService;
        this.workItemNotificationsDatabaseService = workItemNotificationsDatabaseService;
        this.workItemsNotificationClient = workItemsNotificationClient;
        this.workItemsClient = workItemsClient;
        this.activityLogService = activityLogService;
        this.slackQuestionnaireCacheService = slackQuestionnaireCacheService;
        this.slackBotClientFactory = slackBotClientFactory;
        this.inventoryService = inventoryService;
        this.slackSubmitQuestionnaireService = slackSubmitQuestionnaireService;
        this.questionnaireNotificationClient = questionnaireNotificationClient;
        this.slackWorkItemCacheService = slackWorkItemCacheService;
        this.slackUsersDatabaseService = slackUsersDatabaseService;
        this.userClientHelperService = userClientHelperService;
        this.workItemService = workItemService;
    }
    private Optional<UUID> findWIIdUsingMessageId(String company, String messageId, String channelId) throws SQLException {
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 100, null, null, List.of(messageId), List.of(channelId), List.of(NotificationMode.SLACK.toString()));
        if((dbListResponse == null) || (CollectionUtils.isEmpty(dbListResponse.getRecords()))) {
            return Optional.empty();
        }
        return Optional.ofNullable(dbListResponse.getRecords().get(0).getWorkItemId());
    }
    private ImmutablePair<String, Optional<UUID>> findWIIdInAllTenants(String messageId, String channelId, List<SlackTenantLookup> slackTenantLookups) throws SQLException {
        for(SlackTenantLookup slackTenantLookup : slackTenantLookups) {
            String company = slackTenantLookup.getTenantName();
            //Using message id, find the WI id
            Optional<UUID> optionalWIId = findWIIdUsingMessageId(company, messageId, channelId);
            if(optionalWIId.isPresent()) {
                log.info("wi found, messageId {}, tenantName {}, wiId {}", messageId, company, optionalWIId.get());
                return ImmutablePair.of(company, optionalWIId);
            } else {
                log.info("wi not found in current tenant, messageId {}, tenantName {}", messageId, company);
            }
        }
        log.info("wi not found in any tenants, messageId = {}", messageId);
        return ImmutablePair.of(null, Optional.empty());
    }

    private String extractMessage(SlackInteractiveEvent.Action action, Map<String, SlackInteractiveEvent.Block> blocksMap) {
        if((!"forward_snippet_ticket".equals(action.getBlockId())) || (!"forward_snippet_ticket".equals(action.getActionId()))) {
            log.debug("Event is not for snippet workitem");
            return null;
        }
        SlackInteractiveEvent.Block requestorMessageBlock = blocksMap.getOrDefault("NOOP_REQUESTORMESSAGE", null);
        log.debug("requestorMessageBlock = {}", requestorMessageBlock);
        if(requestorMessageBlock == null) {
            return null;
        }
        String text = (requestorMessageBlock.getText() == null) ? "" : MoreObjects.firstNonNull(requestorMessageBlock.getText().getText(), "");
        log.debug("text {}", text);
        Matcher matcher = REQUESTOR_MESSAGE_PATTERN.matcher(text);
        if(!matcher.matches()) {
            log.debug("Pattern does not match");
            return null;
        }
        return matcher.group("message");
    }

    private void processWIAction(String company, UUID workItemId, SlackInteractiveEvent.User user,
                                 SlackInteractiveEvent.Action action, List<SlackInteractiveEvent.Block> blocks,
                                 String teamId){
        String actionBlockId = action.getBlockId();
        if(StringUtils.isBlank(actionBlockId)) {
            log.info("Cannot process action, action block id not found, action {}", action);
            return;
        }
        if(CollectionUtils.isEmpty(blocks)) {
            log.info("Cannot process action, blocks list is null or empty, action {}", action);
            return;
        }
        Map<String, SlackInteractiveEvent.Block> blocksMap = blocks.stream().collect(Collectors.toMap(SlackInteractiveEvent.Block::getBlockId, x -> x));
        if(!blocksMap.containsKey(actionBlockId)) {
            log.info("Cannot process action, action block id not found in blocks, action {}", action);
            return;
        }
        SlackInteractiveEvent.Block block = blocksMap.get(actionBlockId);
        String eventText = (block.getText() != null) ? block.getText().getText() : null;
        if(StringUtils.isBlank(eventText)) {
            log.info("Cannot process action, block event text is blank, action {}, block {}", action, block);
            return;
        }

        String slackUserId = (user != null) ? user.getId() : null;
        String slackUsersFullName = (user != null) ? user.getName() : null;
        String slackUserName = (user != null) ? user.getUsername() : null;
        String slackUser = MoreObjects.firstNonNull(MoreObjects.firstNonNull(slackUsersFullName, slackUserName), "unavailable");

        String eventType = null;
        if(("Change ticket status".equals(eventText)) || ("Change+ticket+status".equals(eventText)) || (("change_status_snippet_ticket".equals(actionBlockId)) && ("change_status_snippet_ticket".equals(action.getActionId())))) {
            eventType = "CHANGE_TICKET_STATUS";
            String newStatus = (action.getSelectedOption() != null) ? action.getSelectedOption().getValue() : null;
            if(StringUtils.isBlank(newStatus)) {
                log.info("Cannot process action, newStatus is blank, action {}, block {}", action, block);
                return;
            }
            //change ticket status
            try {
                workItemsClient.changeState(company, workItemId, newStatus);
                log.info("Successfully updated work item status, company {}, workitem id {}, newStatus {}", company, workItemId, newStatus);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(workItemId.toString())
                        .email("Slack user - " + slackUser)
                        .targetItemType(ActivityLog.TargetItemType.TICKET)
                        .body("Work item status change")
                        .details(Collections.singletonMap("state_name", newStatus))
                        .action(ActivityLog.Action.EDITED)
                        .build());
            } catch (SQLException | InternalApiClientException e) {
                log.error("Error updating wi status!", e);
            }
        } else if(("Forward this ticket".equals(eventText)) || ("Forward+this+ticket".equals(eventText)) || (("forward_snippet_ticket".equals(actionBlockId)) && ("forward_snippet_ticket".equals(action.getActionId())))) {
            eventType = "FORWARD_TICKET";
            List<String> newRecipients = (action.getSelectedConversations() != null) ? action.getSelectedConversations() : null;
            if(CollectionUtils.isEmpty(newRecipients)) {
                log.info("finished processing action, newRecipients is empty, action {}, block {}",action, block);
                return;
            }
            String message = extractMessage(action, blocksMap);
            log.debug("message {}", message);

            //change ticket assigne
            WorkItemNotificationRequest.WorkItemNotificationRequestBuilder bldr = WorkItemNotificationRequest.builder()
                    .workItemId(workItemId)
                    .mode(NotificationMode.SLACK)
                    .recipients(newRecipients)
                    .requestorType(NotificationRequestorType.SLACK_USER)
                    .requestorId(slackUserId)
                    .requestorName(slackUsersFullName);
            if(StringUtils.isNotBlank(message)) {
                bldr.message(message);
            }

            WorkItemNotificationRequest workItemNotificationRequest = bldr.build();
            log.debug("workItemNotificationRequest {}", workItemNotificationRequest);
            try {
                workItemsNotificationClient.queueRequest(company, workItemNotificationRequest);
                log.info("Successfully queue work item, notification request company {}, workitem id {}, newRecipients {}", company, workItemId, newRecipients);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(workItemId.toString())
                        .email("Slack user - " + slackUser)
                        .targetItemType(ActivityLog.TargetItemType.TICKET)
                        .body("Work item forwarded on slack")
                        .details(Collections.singletonMap("new_recipients", newRecipients))
                        .action(ActivityLog.Action.EDITED)
                        .build());
            } catch (SQLException | InternalApiClientException e) {
                log.error("Error sending queue wi notification request!", e);
            }
        } else if (("Assign ticket".equals(eventText)) || ("Assign+ticket".equals(eventText))
                || (("assign_snippet_ticket".equals(actionBlockId))
                        && ("assignee_select_snippet_ticket".equals(action.getActionId())))) {
            eventType = "ASSIGN_TICKET";
            log.info("processWIAction: processing {} event, company {}, action {}", eventType, company, action);
            String newAssignnee = action.getSelectedUser();
            if (StringUtils.isBlank(newAssignnee)) {
                log.info("processWIAction: Cannot process action, newAssignnee is blank, action {}, block {}", action, block);
                return;
            }
            final Optional<SlackUser> newAssigneeOpt = getUserFromSlackId(company, teamId, newAssignnee);
            if (newAssigneeOpt.isEmpty()) {
                log.info("processWIAction: couldn't load email for slack user id {} in company {}, teamId {}",
                        newAssignnee, company, teamId);
                return;
            }
            final SlackUser newAssignee = newAssigneeOpt.get();
            final String assigneeEmail = newAssignee.getEmail();
            try {
                final Optional<User> userByEmail = userClientHelperService.getUserByEmail(company, assigneeEmail);
                final String id;
                if (userByEmail.isEmpty()) {
                    Pair<String, String> firstAndLastName = getFirstNameAndLastName(company, newAssignnee, newAssignee);
                    final User newUser = userClientHelperService.createUser(company, "Slack user - " + slackUser,
                            new ModifyUserRequest(assigneeEmail, firstAndLastName.getLeft(), firstAndLastName.getRight(),
                                    RoleType.LIMITED_USER.name(), false, true, true, null, null, null, null, null));
                    id = newUser.getId();
                } else {
                    id = userByEmail.get().getId();
                }
                String requester = slackUser;
                String requesterUserType = null;
                if (user != null) {
                    Optional<String> requesterEmail = getUserFromSlackId(company, teamId, user.getId())
                            .map(SlackUser::getEmail);
                    if (requesterEmail.isPresent()) {
                        requester = requesterEmail.get();
                        requesterUserType = userClientHelperService.getUserByEmail(company, requester)
                                .map(User::getUserType)
                                .map(RoleType::toString)
                                .orElse(null);
                    }
                }
                workItemService.updateWorkItem(company, requester, requesterUserType, workItemId,
                        WorkItem.builder()
                                .assignees(List.of(WorkItem.Assignee.builder()
                                        .userId(id)
                                        .userEmail(assigneeEmail)
                                        .build()))
                        .build());
                log.info("processWIAction: updated WI {} with assignee {} in company {}, team {}", workItemId,
                        assigneeEmail, company, teamId);
            } catch (IOException | SQLException | EmailException e) {
                log.error(String.format("processWIAction: Error updating assignee for workitem: %s to user %s, for company %s, team %s",
                        workItemId, assigneeEmail, company, teamId), e);
            }
        } else {
            eventType = null;
            log.info("Cannot process action, eventType is not valid, action {}, block {}", action, block);
        }
    }

    private Pair<String, String> getFirstNameAndLastName(String company, String newRecipient, SlackUser newAssignee) {
        Pair<String, String> firstAndLastName;
        try {
            firstAndLastName = slackBotClientFactory.get(SLACK_INTEGRATION_CACHE.get(company))
                    .getUserInfo(newRecipient)
                    .map(SlackApiUser::getProfile)
                    .map(profile -> ImmutablePair.of(profile.getFirstName(), profile.getLastName()))
                    .orElse(ImmutablePair.nullPair());
        } catch (SlackClientException | ExecutionException e) {
            firstAndLastName = ImmutablePair.nullPair();
            log.warn("Error getting user info", e);
        }
        if (firstAndLastName.getLeft() == null) {
            firstAndLastName = getFirstAndLastName(newAssignee.getRealNameNormalized());
        }
        return firstAndLastName;
    }

    private Pair<String, String> getFirstAndLastName(String fullName) {
        final int i = fullName.indexOf(" ");
        if (i == -1) {
            return ImmutablePair.of(fullName, "");
        } else {
            return ImmutablePair.of(fullName.substring(0, i), fullName.substring(i + 1));
        }
    }

    private Optional<SlackUser> getUserFromSlackId(String company, String teamId, String userId) {
        Optional<SlackUser> slackUserOpt = Optional.empty();
        try {
            slackUserOpt = slackUsersDatabaseService.lookup(company, teamId, userId)
                    .stream()
                    .findFirst();
        } catch (SQLException e) {
            log.warn(String.format("getUserFromId: Error looking up for slack user %s company %s teamId %s.",
                    userId, company, teamId), e);
        }
        try {
            if (slackUserOpt.isEmpty() || slackUserOpt.map(SlackUser::getEmail).filter(StringUtils::isNotEmpty).isEmpty()) {
                Optional<SlackApiUser> userInfoOpt = slackBotClientFactory.get(SLACK_INTEGRATION_CACHE.get(company))
                        .getUserInfo(userId);
                if (userInfoOpt.isPresent()) {
                    final SlackApiUser userInfo = userInfoOpt.get();
                    slackUserOpt = Optional.of(insertSlackUser(company, userInfo));
                }
            }
        } catch (ExecutionException | SlackClientException | SQLException e) {
            log.error(String.format("getUserFromId: Error fetching user info from slack for userId %s, company %s, teamId %s",
                    userId, company, teamId), e);
        }
        return slackUserOpt;
    }

    private SlackUser insertSlackUser(String company, SlackApiUser slackApiUser) throws SQLException {
        SlackUser slackUser = SlackUser.builder()
                .teamId(slackApiUser.getTeamId())
                .userId(slackApiUser.getId())
                .realNameNormalized(Optional.ofNullable(slackApiUser.getProfile())
                        .map(SlackApiUser.Profile::getRealNameNormalized)
                        .orElse(""))
                .username(slackApiUser.getName())
                .email(Optional.ofNullable(slackApiUser.getProfile())
                        .map(SlackApiUser.Profile::getEmail)
                        .orElse(null))
                .build();
            final String id = slackUsersDatabaseService.upsert(company, slackUser);
            slackUser = slackUser.toBuilder().id(UUID.fromString(id)).build();
            return slackUser;
    }

    private void processWIActions(String company, UUID workItemId, SlackInteractiveEvent slackInteractivityEvent,
                                  String teamId){
        if(CollectionUtils.isEmpty(slackInteractivityEvent.getActions())) {
            return;
        }
        for(SlackInteractiveEvent.Action action : slackInteractivityEvent.getActions()) {
            processWIAction(company, workItemId, slackInteractivityEvent.getUser(), action,
                    slackInteractivityEvent.getMessage().getBlocks(), teamId);
        }
    }

    private void processWIInteractivityEvent(SlackInteractiveEvent slackInteractivityEvent) throws SQLException {
        //Get team id and message id
        String teamId = (slackInteractivityEvent.getTeam() != null) ? slackInteractivityEvent.getTeam().getId() : null;
        if(StringUtils.isBlank(teamId)) {
            log.info("Cannot process Interactive Event teamId not found, tiggerId {}", slackInteractivityEvent.getTriggerId());
            return;
        }

        String messageId = (slackInteractivityEvent.getMessage() != null) ? slackInteractivityEvent.getMessage().getTs() : null;
        if(StringUtils.isBlank(messageId)) {
            log.info("Cannot process Interactive Event messageId not found, messageId is blank, tiggerId {}", slackInteractivityEvent.getTriggerId());
            return;
        }

        String channelId = (slackInteractivityEvent.getChannel() != null) ? slackInteractivityEvent.getChannel().getId() : null;
        if(StringUtils.isBlank(channelId)) {
            log.info("Cannot process Interactive Event messageId not found, channelId is blank, tiggerId {}", slackInteractivityEvent.getTriggerId());
            return;
        }

        //Using team id, find the company
        List<SlackTenantLookup> slackTenantLookups = slackTenantLookupDatabaseService.lookup(teamId);
        if(CollectionUtils.isEmpty(slackTenantLookups)) {
            log.info("Cannot process Interactive Event, slackTenantLookups not found for team id, tiggerId {}, teamId {}", slackInteractivityEvent.getTriggerId(), teamId);
            return;
        }

        //Find WI Id using messageId and slackTenantLookups
        ImmutablePair<String, Optional<UUID>> pair = findWIIdInAllTenants(messageId, channelId, slackTenantLookups);;
        Optional<UUID> optionalWiId = pair.getRight();
        if(optionalWiId.isEmpty()) {
            log.info("Cannot process Interactive Event, ");
            return;
        }
        String company = pair.getLeft();

        //Get user info

        //parse event type to change ticket status or ticket assigne
        processWIActions(company, optionalWiId.get(), slackInteractivityEvent, teamId);
    }

    private boolean isInteractiveEventActionForQuestionnaire(SlackInteractiveEvent.Action action) {
        boolean c1 = "questionnaire_notification".equals(action.getBlockId());
        boolean c2 = "edit_questionnaire".equals(action.getActionId());
        log.info("c1 {}, c2{}", c1, c2);
        return ("questionnaire_notification".equals(action.getBlockId())) && ("edit_questionnaire".equals(action.getActionId()));
    }

    private boolean isInteractiveEventForQuestionnaire(SlackInteractiveEvent slackInteractivityEvent) {
        if(CollectionUtils.isEmpty(slackInteractivityEvent.getActions())) {
            log.info("Interactive Events does not have actions, it is not for Questionnaire");
            return false;
        }
        for(SlackInteractiveEvent.Action action : slackInteractivityEvent.getActions()) {
            if(isInteractiveEventActionForQuestionnaire(action)) {
                log.info("Interactive Events actions, is for Questionnaire");
                return true;
            }
        }
        return false;
    }

    private boolean isInteractiveEventActionForWIViewTextAttachment(SlackInteractiveEvent.Action action) {
        boolean c1 = "workitem_snippet".equals(action.getBlockId());
        boolean c2 = "view_wi_text_attachment".equals(action.getActionId());
        log.info("c1 {}, c2{}", c1, c2);
        return ("workitem_snippet".equals(action.getBlockId())) && ("view_wi_text_attachment".equals(action.getActionId()));
    }

    private boolean isInteractiveEventForWIViewTextAttachment(SlackInteractiveEvent slackInteractivityEvent) {
        if(CollectionUtils.isEmpty(slackInteractivityEvent.getActions())) {
            log.info("Interactive Events does not have actions, it is not for Questionnaire");
            return false;
        }
        for(SlackInteractiveEvent.Action action : slackInteractivityEvent.getActions()) {
            if(isInteractiveEventActionForWIViewTextAttachment(action)) {
                log.info("Interactive Events actions, is for WorkItem View Text Attachment");
                return true;
            }
        }
        return false;
    }


    private void processQuestionnaireAction(String triggerId, SlackInteractiveEvent.Action action) {
        if(!isInteractiveEventActionForQuestionnaire(action)) {
            log.info("Slack Questionnaire Interactive Event action is not valid {}", action);
            return;
        }
        String value = action.getValue();
        Optional<ImmutablePair<String, String>> result = QuestionnaireSlackMessageUtils.parseQuestionnaireSlackCallbackId(value);
        if(result.isEmpty()) {
            log.info("Slack Questionnaire Interactive Event action could not parse callback id {}", action);
            return;
        }
        String company = result.get().getLeft();
        UUID questionnaireId = UUID.fromString(result.get().getRight());
        log.info("company {}, questionnaireId {}",company, questionnaireId);
        Optional<String> optionalModalMessage = slackQuestionnaireCacheService.getQuestionnaireMessage(company, questionnaireId);
        if(optionalModalMessage.isEmpty()) {
            log.info("Slack Questionnaire Interactive Event, modal message not found in cache! company {}, questionnaireId {}",company, questionnaireId);
            //Queue again
            try {
                QuestionnaireNotificationClient.QueueResponse response = questionnaireNotificationClient.rebuildCache(company, questionnaireId);
                log.info("Successfully queued cache rebuild for company {}, questionnaireId {}, response {} !", company, questionnaireId, response);
            } catch (InternalApiClientException e) {
                log.error("Error queuing cache rebuild for company {}, questionnaireId {}!", company, questionnaireId);
            }
            return;
        }
        String modalMessage = optionalModalMessage.get();
        log.info("modalMessage = {}", modalMessage);
        log.info("triggerId = {}", triggerId);
        try {
            log.info("Sending answer questionnaire inline modal");
            String modalId = slackBotClientFactory.get(SLACK_INTEGRATION_CACHE.get(company)).openView(triggerId, modalMessage).getView().getId();
            log.info("Sent answer questionnaire inline modal id {}", modalId);
        } catch (SlackClientException | ExecutionException e) {
            log.error("Error sending answer questionnaire inline modal!", e);
        }
    }

    private void processQuestionnaireInteractivityEvent(SlackInteractiveEvent slackInteractivityEvent) {
        if(CollectionUtils.isEmpty(slackInteractivityEvent.getActions())) {
            log.info("Slack Questionnaire Interactive Event does not have actions");
            return;
        }
        if(StringUtils.isBlank(slackInteractivityEvent.getTriggerId())) {
            log.info("Slack Questionnaire Interactive Event does not have triggr id");
            return;
        }
        for(SlackInteractiveEvent.Action action : slackInteractivityEvent.getActions()) {
            processQuestionnaireAction(slackInteractivityEvent.getTriggerId(), action);
        }
    }

    //----------------- HERE
    private void processWorkItemViewTextAttachmentAction(String triggerId, SlackInteractiveEvent.Action action) {
        if(!isInteractiveEventActionForWIViewTextAttachment(action)) {
            log.info("Slack WorkItem View Text Attachment Interactive Event action is not valid {}", action);
            return;
        }
        String value = action.getValue();
        Optional<WorkItemSlackMessageUtils.WorkItemAttchmentMetadata> result = WorkItemSlackMessageUtils.parseQuestionnaireSlackCallbackId(value);
        if(result.isEmpty()) {
            log.info("Slack WorkItem View Text Attachment Interactive Event action could not parse callback id {}", action);
            return;
        }
        String company = result.get().getCompany();
        UUID workItemId = UUID.fromString(result.get().getWorkItemId());
        UUID uploadId = UUID.fromString(result.get().getUploadId());
        log.info("company {}, workItemId {}, uploadId {}",company, workItemId, uploadId);
        Optional<String> optionalModalMessage = slackWorkItemCacheService.getWITextAttachmentMessage(company, workItemId, uploadId);
        if(optionalModalMessage.isEmpty()) {
            log.info("Slack WorkItem View Text Attachment Interactive Event, modal message not found in cache! company {}, workItemId {}, uploadId {}",company, workItemId, uploadId);
            //Queue again
            try {
                Id response = workItemsNotificationClient.rebuildTextAttachmentCache(company, workItemId, uploadId);
                log.info("Successfully queued cache rebuild for company {}, workItemId {}, uploadId {}, response {} !", company, workItemId, uploadId, response);
            } catch (InternalApiClientException e) {
                log.error("Error queuing cache rebuild for company {}, workItemId {}, uploadId {}!", company, workItemId, uploadId);
            }
            return;
        }
        String modalMessage = optionalModalMessage.get();
        log.info("modalMessage = {}", modalMessage);
        log.info("triggerId = {}", triggerId);
        try {
            log.info("Sending view work item text attachment modal");
            String modalId = slackBotClientFactory.get(SLACK_INTEGRATION_CACHE.get(company)).openView(triggerId, modalMessage).getView().getId();
            log.info("Sent view work item text attachment modal id {}", modalId);
        } catch (SlackClientException | ExecutionException e) {
            log.error("Error sending answer questionnaire inline modal!", e);
        }
    }

    private void processWorkItemViewTextAttachmentInteractivityEvent(SlackInteractiveEvent slackInteractivityEvent) {
        if(CollectionUtils.isEmpty(slackInteractivityEvent.getActions())) {
            log.info("Slack WorkItem View Text Attachment Interactive Event does not have actions");
            return;
        }
        if(StringUtils.isBlank(slackInteractivityEvent.getTriggerId())) {
            log.info("Slack WorkItem View Text Attachment Interactive Event does not have triggr id");
            return;
        }
        for(SlackInteractiveEvent.Action action : slackInteractivityEvent.getActions()) {
            processWorkItemViewTextAttachmentAction(slackInteractivityEvent.getTriggerId(), action);
        }
    }
    //-----------------

    @Async("slackInteractivityEventTaskExecutor")
    public void processSlackInteractivityEvent(String interactivityEvent) throws JsonProcessingException, SQLException, InternalApiClientException {
        //Decode Event
        log.info("interactivityEvent = {}", interactivityEvent);
        if(!interactivityEvent.startsWith(PAYLOAD_EQUALS)) {
            log.info("Cannot process Interactive Event, it does not start with payload=");
            return;
        }
        String interactivityEventPayload = interactivityEvent.substring(PAYLOAD_EQUALS.length());
        log.info("interactivityEventPayload = {}", interactivityEventPayload);

        String eventDecodedString = URLDecoder.decode(interactivityEventPayload, StandardCharsets.UTF_8);
        log.info("eventDecodedString = {}", eventDecodedString);

        //Parse Event
        SlackInteractiveEvent slackInteractivityEvent = objectMapper.readValue(eventDecodedString, SlackInteractiveEvent.class);

        if(!slackVerificationToken.equals(slackInteractivityEvent.getToken())) {
            log.error("Will not process Interactive Event, token in event does not match app verification token, event token {}",slackInteractivityEvent.getToken());
            return;
        }

        if("view_submission".equals(slackInteractivityEvent.getType())) {
            slackSubmitQuestionnaireService.processSubmitQuestionnaireEvent(slackInteractivityEvent);
        } else {
            if(isInteractiveEventForQuestionnaire(slackInteractivityEvent)) {
                log.info("Event is for Questionnaire");
                processQuestionnaireInteractivityEvent(slackInteractivityEvent);
            } else if (isInteractiveEventForWIViewTextAttachment(slackInteractivityEvent)) {
                log.info("Event is for WorkItem View Text Attachment");
                processWorkItemViewTextAttachmentInteractivityEvent(slackInteractivityEvent);
            } else {
                log.info("Event is for WorkItem");
                processWIInteractivityEvent(slackInteractivityEvent);
            }
        }
    }

    private Optional<IntegrationKey> findSlackIntegration(String tenantId) throws InventoryException {
        DbListResponse<Integration> integrations = inventoryService.listIntegrationsByFilters(tenantId, "slack",
                null, null);
        if (integrations == null || CollectionUtils.isEmpty(integrations.getRecords())) {
            return Optional.empty();
        }
        return integrations.getRecords().stream()
                .filter(integration -> "slack".equalsIgnoreCase(integration.getApplication()))
                .filter(integration -> "ACTIVE".equalsIgnoreCase(integration.getStatus()))
                .map(integration -> IntegrationKey.builder()
                        .tenantId(tenantId)
                        .integrationId(integration.getId())
                        .build())
                .findAny();
    }
}
