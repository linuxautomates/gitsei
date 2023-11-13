package io.levelops.api.controllers;

import io.levelops.commons.databases.services.GenericEventDatabaseService;
import io.levelops.web.util.SpringUtils;
import io.propelo.commons.generic_events.models.GenericEventRequest;
import io.propelo.commons.generic_events.models.GenericEventResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@Log4j2
@RequestMapping("/v1/generic-events")
public class GenericEventsRequestController {
    private final GenericEventDatabaseService genericEventDatabaseService;

    @Autowired
    public GenericEventsRequestController(GenericEventDatabaseService genericEventDatabaseService) {
        this.genericEventDatabaseService = genericEventDatabaseService;
    }

    @PostMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<GenericEventResponse>> createGenericRequest(@SessionAttribute("company") final String company,
                                                                                     @RequestBody GenericEventRequest genericEventRequest) {
        return SpringUtils.deferResponse(() -> {
            log.info("generic-events request {}", genericEventRequest);
            String id = genericEventDatabaseService.insert(company, genericEventRequest);
            GenericEventResponse response = GenericEventResponse.builder().id(id).build();
            log.info("generic-events response {}", response);
            return ResponseEntity.accepted().body(response);
        });
    }
}
