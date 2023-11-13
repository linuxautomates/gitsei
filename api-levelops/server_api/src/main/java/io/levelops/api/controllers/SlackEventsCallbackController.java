package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.model.slack.SlackEventPayload;
import io.levelops.api.services.SlackEventsService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@Log4j2
@RequestMapping("/webhooks/slack/action-endpoint")
public class SlackEventsCallbackController {
    private static final String SUCCESS = "success";
    private final ObjectMapper objectMapper;
    private final SlackEventsService slackEventsService;

    @Autowired
    public SlackEventsCallbackController(ObjectMapper objectMapper, SlackEventsService slackEventsService) {
        this.objectMapper = objectMapper;
        this.slackEventsService = slackEventsService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "text/plain")
    public DeferredResult<ResponseEntity<String>> receiveEvent(@RequestBody String eventPayloadStr) {
        log.info("eventPayloadStr = {}", eventPayloadStr);
        return SpringUtils.deferResponse(() -> {
            try {
                SlackEventPayload eventPayload = objectMapper.readValue(eventPayloadStr, SlackEventPayload.class);
                if ("url_verification".equals(eventPayload.getType())) {
                    return ResponseEntity.ok().body(eventPayload.getChallenge());
                }
                log.debug("eventPayloadStr = {}", eventPayloadStr);
                log.info("event is real event");
                slackEventsService.processEvent(eventPayload);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing Slack Event! eventPayloadStr = {}", eventPayloadStr, e);
            } catch (IllegalAccessException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(e.getMessage());
            }
            return ResponseEntity.ok().body(SUCCESS);
        });
    }
}
