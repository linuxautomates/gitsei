package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.events.models.EventsClientException;
import io.levelops.internal_api.services.JenkinsPluginJobRunCompleteService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class JenkinsPluginJobRunCompleteHandler implements GenericRequestHandler {
    private static final String REQUEST_TYPE = "JenkinsPluginJobRunComplete";
    private static final String RESPONSE_TYPE = "JenkinsPluginJobRunCompleteResponse";
    private final ObjectMapper mapper;
    private final JenkinsPluginJobRunCompleteService jenkinsPluginJobRunCompleteService;

    @Autowired
    public JenkinsPluginJobRunCompleteHandler(ObjectMapper mapper, JenkinsPluginJobRunCompleteService jenkinsPluginJobRunCompleteService) {
        this.mapper = mapper;
        this.jenkinsPluginJobRunCompleteService = jenkinsPluginJobRunCompleteService;
    }

    @Override
    public String getRequestType() {
        return REQUEST_TYPE;
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest) throws JsonProcessingException, EventsClientException {
        return handleRequest(company, genericRequest, null);
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest, MultipartFile zipFile) throws JsonProcessingException, EventsClientException {
        UUID resultId = UUID.randomUUID();
        log.info("requestType {}, company {}, uuid={}", REQUEST_TYPE, company, resultId);
        log.debug("genericRequest = {}", genericRequest);
        jenkinsPluginJobRunCompleteService.submitJenkinsResultsForPreProcess(company, resultId, genericRequest.getPayload(), zipFile);
        Map<String, Object> payload = Map.of("run_ids", Collections.emptyList());
        GenericResponse genericResponse = GenericResponse.builder()
                .responseType(RESPONSE_TYPE)
                .payload(mapper.writeValueAsString(payload))
                .build();
        log.debug("genericResponse = {}", genericResponse);
        return genericResponse;
    }
}
