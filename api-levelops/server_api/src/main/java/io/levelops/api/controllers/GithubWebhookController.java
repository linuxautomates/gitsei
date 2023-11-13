package io.levelops.api.controllers;

import io.levelops.api.services.GithubWebhookService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/webhooks/github")
@SuppressWarnings("unused")
public class GithubWebhookController {

    private final GithubWebhookService webhookService;
    
    @Autowired
    public GithubWebhookController(GithubWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{tenantId}/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> postEvents(@PathVariable("tenantId") String company,
                                                             @PathVariable("id") String integrationId,
                                                             @RequestBody String event,
                                                             @RequestHeader Map<String, String> header) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(webhookService.onEvent(company, integrationId, event, header)));
    }
}
