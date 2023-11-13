package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.services.SlackInteractivityEventService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@Log4j2
@SuppressWarnings("unused")
@RequestMapping("/webhooks/slack/interactivity")
public class SlackInteractivityCallbackController {
    private static final String SUCCESS = "success";
    private static final String UPDATE_MODAL = "{\"response_action\": \"update\",\"view\": {\"type\": \"modal\",\"title\": {\"type\": \"plain_text\",\"text\": \"Assessment submitted\"},\"blocks\": [{\"type\": \"section\",\"text\": {\"type\": \"plain_text\",\"text\": \"Completed assessment has been submitted\"}}]}}";
    private static final String UPDATE_MODAL_1 = "{\"response_action\": \"update\",\"view\": {\"type\": \"modal\",\"title\": {\"type\": \"plain_text\",\"text\": \"Updated view\"},\"blocks\": [{\"type\": \"section\",\"text\": {\"type\": \"plain_text\",\"text\": \"I've changed and I'll never be the same. You must believe me.\"}}]}}";
    private static final String CLEAR_MODAL = "{\"response_action\": \"clear\"}";

    private final ObjectMapper objectMapper;
    private final SlackInteractivityEventService slackInteractivityEventService;

    @Autowired
    public SlackInteractivityCallbackController(ObjectMapper objectMapper, SlackInteractivityEventService slackInteractivityEventService) {
        this.objectMapper = objectMapper;
        this.slackInteractivityEventService = slackInteractivityEventService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "text/plain")
    public DeferredResult<ResponseEntity<String>> receiveInteractivityEvent(@RequestBody String interactivityEventPayloadStr) {
        return SpringUtils.deferResponse(() -> {
            slackInteractivityEventService.processSlackInteractivityEvent(interactivityEventPayloadStr);
            return ResponseEntity.ok().body(null);
        });
    }
}
