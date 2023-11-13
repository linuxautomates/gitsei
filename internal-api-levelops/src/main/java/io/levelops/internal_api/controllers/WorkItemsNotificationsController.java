package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import io.levelops.commons.databases.models.database.WorkItemNote;
import io.levelops.commons.databases.models.database.WorkItemNotificationRequest;
import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.levelops.commons.databases.models.database.slack.SlackUserResult;
import io.levelops.commons.databases.services.SlackUsersDatabaseService;
import io.levelops.commons.databases.services.WorkItemNotesService;
import io.levelops.ingestion.models.Job;
import io.levelops.internal_api.services.SlackInteractiveMessageResultService;
import io.levelops.internal_api.services.WorkItemService;
import io.levelops.notification.models.SlackNotificationResult;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RestController
@RequestMapping("/internal/v1/tenants/{company}/workitems_notifications")
public class WorkItemsNotificationsController {
    private final ObjectMapper objectMapper;
    private final WorkItemService workItemService;
    private final SlackUsersDatabaseService slackUsersDatabaseService;
    private final WorkItemNotesService workItemNotesService;
    private final SlackInteractiveMessageResultService slackInteractiveMessageResultService;

    @Autowired
    public WorkItemsNotificationsController(ObjectMapper objectMapper, WorkItemService workItemService, SlackUsersDatabaseService slackUsersDatabaseService, WorkItemNotesService workItemNotesService, SlackInteractiveMessageResultService slackInteractiveMessageResultService) {
        this.objectMapper = objectMapper;
        this.workItemService = workItemService;
        this.slackUsersDatabaseService = slackUsersDatabaseService;
        this.workItemNotesService = workItemNotesService;
        this.slackInteractiveMessageResultService = slackInteractiveMessageResultService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> queueWorkItemNotification(@PathVariable("company") final String company,
                                                                      @RequestBody WorkItemNotificationRequest workItemNotificationRequest) {
        return SpringUtils.deferResponse(() -> {
            SlackNotificationResult result = workItemService.queueNotification(company, workItemNotificationRequest);
            return ResponseEntity.ok().body(Map.of("id", result.getJobId()));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/callbacks/slack", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, List<String>>>> receiveFailureTriageSlackCallback(@PathVariable("company") final String company,
                                                                                               @RequestBody Job job) {
        return SpringUtils.deferResponse(() -> {
            Object result = job.getResult();
            SlackChatInteractiveMessageResult slackChatInteractiveMessageResult = objectMapper.readValue(objectMapper.writeValueAsString(job.getResult()), SlackChatInteractiveMessageResult.class);
            List<String> ids = slackInteractiveMessageResultService.handleMessageResult(company, slackChatInteractiveMessageResult);
            return ResponseEntity.ok().body(Map.of("ids", ids));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/callbacks/slack_fetch_user", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> receiveSlackFetchUserCallback(@PathVariable("company") final String company,
                                                                                                       @RequestBody Job job) {
        return SpringUtils.deferResponse(() -> {
            log.debug("Received slack fetch user callback company {}, job {}", company, job);
            Object result = job.getResult();
            SlackUserResult slackUserResult = objectMapper.readValue(objectMapper.writeValueAsString(job.getResult()), SlackUserResult.class);
            if ((!Boolean.TRUE.equals(slackUserResult.getFound())) || (slackUserResult.getSlackUser() == null)) {
                log.info("Slack user not found company {}, slackUserId {}, workItemNoteId {}", company, slackUserResult.getSlackUserId(), slackUserResult.getWorkItemNoteId());
                return ResponseEntity.ok().body(Map.of());
            }

            String workItemNoteId = slackUserResult.getWorkItemNoteId().toString();
            SlackUser slackUser = slackUserResult.getSlackUser();
            String slackUserDbId = slackUsersDatabaseService.upsert(company, slackUser);
            log.debug("successfully persisted slack user to db slackUserDbId {}, slackUser {}", slackUserDbId, slackUser);
            String userRealName = slackUser.getRealNameNormalized();

            Optional<WorkItemNote> opt = workItemNotesService.get(company, workItemNoteId);
            if(opt.isEmpty()) {
                log.info("Work Item note not found company {}, workItemNoteId {}", company, workItemNoteId);
                return ResponseEntity.ok().body(Map.of());
            }
            WorkItemNote workItemNote = opt.get();
            log.debug("workItemNote = {}", workItemNote);
            WorkItemNote updatedWorkItemNote = workItemNote.toBuilder().creator("Slack User - " + userRealName).build();
            log.debug("updatedWorkItemNote = {}", updatedWorkItemNote);
            Boolean updateResult = workItemNotesService.update(company, updatedWorkItemNote);
            log.debug("updateResult = {}", updateResult);

            return ResponseEntity.ok().body(Map.of("id", workItemNoteId));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/slack/rebuild_text_attachments_cache/{workitem_id}/{upload_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> rebuildWorkItemViewTextAttachmentSlackMessageCache(@PathVariable("company") final String company,
                                                                                                                  @PathVariable("workitem_id") final String workItemId,
                                                                                                                  @PathVariable("upload_id") final String uploadId) {
        return SpringUtils.deferResponse(() -> {
            workItemService.rebuildWorkItemViewTextAttachmentSlackCache(company, workItemId, uploadId);
            return ResponseEntity.ok().body(Map.of("id", uploadId));
        });
    }

}
