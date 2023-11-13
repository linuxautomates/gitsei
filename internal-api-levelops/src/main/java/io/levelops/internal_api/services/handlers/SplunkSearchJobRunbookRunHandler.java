package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Event;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link GenericRequestHandler} for handling Splunk alerts
 */
@Log4j2
@Service
public class SplunkSearchJobRunbookRunHandler implements GenericRequestHandler {

    private static final String SPLUNK_SEARCH_JOB_RUNBOOK_RUN_REQUEST = "SplunkSearchJobRunbookRunRequest";
    private static final String SPLUNK_SEARCH_JOB_RUNBOOK_RUN_RESPONSE = "SplunkSearchJobRunbookRunResponse";
    private static final String RUN_IDS = "run_ids";

    private final ObjectMapper objectMapper;
    private final EventsClient eventsClient;

    public SplunkSearchJobRunbookRunHandler(ObjectMapper objectMapper, EventsClient eventsClient) {
        this.objectMapper = objectMapper;
        this.eventsClient = eventsClient;
    }

    @Override
    public String getRequestType() {
        return SPLUNK_SEARCH_JOB_RUNBOOK_RUN_REQUEST;
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest)
            throws JsonProcessingException, EventsClientException {
        return handleRequest(company, genericRequest, null);
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest, MultipartFile zipFile) throws JsonProcessingException, EventsClientException {
        Map<String, Object> requestPayload = objectMapper.readValue(genericRequest.getPayload(),
                objectMapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, Object.class));
        Event event = Event.builder().company(company).data(requestPayload).type(EventType.SPLUNK_SEARCH_JOB_COMPLETED)
                .build();
        List<String> runIds = eventsClient.processTriggerEvent(event);
        log.info("handleRequest: created run ids: {}", runIds);
        return GenericResponse.builder()
                .responseType(SPLUNK_SEARCH_JOB_RUNBOOK_RUN_RESPONSE)
                .payload(objectMapper.writeValueAsString(Map.of(RUN_IDS, runIds)))
                .build();
    }
}
