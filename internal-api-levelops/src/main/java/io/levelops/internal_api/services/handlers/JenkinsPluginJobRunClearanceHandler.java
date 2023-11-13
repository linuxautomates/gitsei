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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class JenkinsPluginJobRunClearanceHandler implements GenericRequestHandler {
    private final String requestType = "JenkinsPluginJobRunClearanceRequest";
    private final ObjectMapper mapper;
    private final EventsClient eventsClient;

    @Autowired
    public JenkinsPluginJobRunClearanceHandler(ObjectMapper mapper, EventsClient eventsClient) {
        this.mapper = mapper;
        this.eventsClient = eventsClient;
    }

    @Override
    public String getRequestType() {
        return requestType;
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest) throws JsonProcessingException, EventsClientException {
        return handleRequest(company, genericRequest, null);

    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest, MultipartFile zipFile) throws JsonProcessingException, EventsClientException {
        log.info("requestType {}, company {}", requestType, company);
        log.debug("genericRequest = {}", genericRequest);

        Map<String,Object> requestPayload = mapper.readValue(genericRequest.getPayload(), mapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, Object.class));
        log.debug("requestPayload = {}", requestPayload);

        Event triggerEvent = Event.builder().company(company).type(EventType.JENKINS_JOB_RUN_STARTED).data(requestPayload).build();
        log.debug("triggerEvent = {}", triggerEvent);

        List<String> runIds = eventsClient.processTriggerEvent(triggerEvent);
        log.debug("runIds = {}", runIds);

        Map<String ,Object> payload = Map.of("run_ids", runIds);
        GenericResponse genericResponse = GenericResponse.builder()
                .responseType("JenkinsPluginJobRunClearanceResponse")
                .payload(mapper.writeValueAsString(payload))
                .build();
        log.debug("genericResponse = {}", genericResponse);
        return genericResponse;
    }
}
