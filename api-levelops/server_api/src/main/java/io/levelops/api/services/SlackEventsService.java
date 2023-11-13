package io.levelops.api.services;

import io.levelops.api.model.slack.SlackEventPayload;
import io.levelops.auth.utils.TenantUtilService;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.databases.models.database.WorkItemNote;
import io.levelops.commons.databases.models.database.WorkItemNotification;
import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.levelops.commons.databases.services.SlackTenantLookupDatabaseService;
import io.levelops.commons.databases.services.SlackUsersDatabaseService;
import io.levelops.commons.databases.services.WorkItemNotesService;
import io.levelops.commons.databases.services.WorkItemNotificationsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.notification.services.NotificationService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Log4j2
@Service
public class SlackEventsService {
    private static final String CONTENT_CANNOT_BE_DISPLAYED = "This content can't be displayed.";
    private final String slackVerificationToken;
    private final SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;
    private final WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService;
    private final WorkItemNotesService notesService;
    private final SlackUsersDatabaseService slackUsersDatabaseService;
    private final NotificationService notificationService;
    private final String internalApiUrl;
    private final TenantUtilService tenantUtilService;

    @Autowired
    public SlackEventsService(@Value("${SLACK_VERIFICATION_TOKEN}") String slackVerificationToken,
                              SlackTenantLookupDatabaseService slackTenantLookupDatabaseService,
                              WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService,
                              WorkItemNotesService notesService, SlackUsersDatabaseService slackUsersDatabaseService,
                              NotificationService notificationService,
                              @Qualifier("internalApiUrl") String internalApiUrl,
                              TenantUtilService tenantUtilService) {
        this.slackVerificationToken = slackVerificationToken;
        this.slackTenantLookupDatabaseService = slackTenantLookupDatabaseService;
        this.workItemNotificationsDatabaseService = workItemNotificationsDatabaseService;
        this.notesService = notesService;
        this.slackUsersDatabaseService = slackUsersDatabaseService;
        this.notificationService = notificationService;
        this.internalApiUrl = internalApiUrl;
        this.tenantUtilService= tenantUtilService;
    }

    private Optional<UUID> findWIIdUsingMessageId(String company, String messageId, String channelId) throws SQLException, ExecutionException, IllegalAccessException {
        tenantUtilService.validateTenant(company);
        DbListResponse<WorkItemNotification> dbListResponse = workItemNotificationsDatabaseService.listByFilter(company, 0, 100, null, null, List.of(messageId), List.of(channelId), List.of(NotificationMode.SLACK.toString()));
        if((dbListResponse == null) || (CollectionUtils.isEmpty(dbListResponse.getRecords()))) {
            return Optional.empty();
        }
        return Optional.ofNullable(dbListResponse.getRecords().get(0).getWorkItemId());
    }
    private ImmutablePair<String, Optional<UUID>> findWIIdInAllTenants(String messageId, String channelId, List<SlackTenantLookup> slackTenantLookups) throws SQLException, ExecutionException, IllegalAccessException {
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

    public void processEvent(SlackEventPayload event) throws SQLException, ExecutionException, IllegalAccessException {
        if(event == null) {
            log.debug("slackEvent is null");
            return;
        }
        if(!slackVerificationToken.equals(event.getToken())) {
            log.error("Will not process Interactive Event, token in event does not match app verification token, event token {}",event.getToken());
            return;
        }
        String teamId = event.getTeamId();
        if(StringUtils.isBlank(teamId)) {
            log.info("cannot process slack event, teamId is blank");
            return;
        }
        if(event.getEvent() == null) {
            log.info("cannot process slack event, event object is null");
            return;
        }
        String type = event.getEvent().getType();
        String reply = event.getEvent().getText();
        String ts = event.getEvent().getTs();
        String threadTs = event.getEvent().getThreadTs();
        String channelId = event.getEvent().getChannel();
        String user = event.getEvent().getUser();
        String botId = event.getEvent().getBotId();
        log.info("type = {}, threadTs = {}, ts = {}, channelId = {}, user = {}, botId = {}", type, threadTs, ts, channelId, user, botId);
        log.trace("reply = {}", reply);
        if((!"message".equals(type)) || (StringUtils.isBlank(threadTs))) {
            log.info("completed processing slack event, no action, type is not message or not a reply");
            return;
        }
        if(StringUtils.isBlank(reply)) {
            log.info("completed processing slack event, no action, reply text is null or empty!, teamId {}, threadTs {}",teamId, threadTs);
            return;
        }
        if ((CONTENT_CANNOT_BE_DISPLAYED.equals(reply)) || (StringUtils.isNotBlank(botId))) {
            log.info("completed processing slack event, no action, reply text is auto generated text or the message was sent by a bot, teamId {}, threadTs {}",teamId, threadTs);
            return;
        }

        //Using team id, find the company
        List<SlackTenantLookup> slackTenantLookups = slackTenantLookupDatabaseService.lookup(teamId);
        if(CollectionUtils.isEmpty(slackTenantLookups)) {
            log.info("Cannot process slack event, slackTenantLookups not found for teamId {}", teamId);
            return;
        }

        //Find WI Id using messageId and slackTenantLookups
        ImmutablePair<String, Optional<UUID>> pair = findWIIdInAllTenants(threadTs, channelId, slackTenantLookups);;
        Optional<UUID> optionalWiId = pair.getRight();
        if(optionalWiId.isEmpty()) {
            log.info("Cannot process slack event, work item not found, teamId {}, messageId {}, channelId {}", teamId, threadTs, channelId);
            return;
        }
        String company = pair.getLeft();

        //Find Users real name
        List<SlackUser> slackUsers = slackUsersDatabaseService.lookup(company, teamId, user);
        String userRealName = (CollectionUtils.isNotEmpty(slackUsers)) ? slackUsers.get(0).getRealNameNormalized() : null;
        log.info("userRealName = {}", userRealName);

        String noteId = notesService.insert(company, WorkItemNote.builder()
                .workItemId(optionalWiId.get().toString())
                .body(reply)
                .creator((userRealName != null) ? ("Slack User - " + userRealName) : "Slack User")
                .build());
        UUID workItemNoteId = UUID.fromString(noteId);

        if(userRealName == null) {
            String callbackUrl = internalApiUrl + "/internal/v1/tenants/" + company + "/workitems_notifications/callbacks/slack_fetch_user";
            log.info("callbackUrl = {}", callbackUrl);

            try {
                notificationService.fetchSlackUser(company, workItemNoteId, user, callbackUrl);
            } catch (IOException e) {
                log.error("Error queueing fetch slack user, company {}, workItemNoteId {}, user {}, callbackUrl {}",company, workItemNoteId, user, callbackUrl);
            }

        }
        log.info("create note for reply note id {}", noteId);
    }
}
