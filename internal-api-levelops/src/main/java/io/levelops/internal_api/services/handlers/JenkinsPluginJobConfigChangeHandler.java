package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.events.models.EventsClientException;
import io.levelops.internal_api.models.JobConfigChangeRequest;
import io.levelops.internal_api.services.JenkinsPluginJobConfigChangeService;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

@Log4j2
@Service
public class JenkinsPluginJobConfigChangeHandler implements GenericRequestHandler {
    private final String requestType = "JenkinsPluginJobConfigChange";
    private final String responseType = "JenkinsPluginJobConfigChangeResponse";
    private final ObjectMapper mapper;
    private final JenkinsPluginJobConfigChangeService jenkinsPluginJobConfigChangeService;

    @Autowired
    public JenkinsPluginJobConfigChangeHandler(ObjectMapper mapper, JenkinsPluginJobConfigChangeService jenkinsPluginJobConfigChangeService) {
        this.mapper = mapper;
        this.jenkinsPluginJobConfigChangeService = jenkinsPluginJobConfigChangeService;
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
        log.info("genericRequest = {}", genericRequest);

        JobConfigChangeRequest jobConfigChangeRequest = mapper.readValue(genericRequest.getPayload(), JobConfigChangeRequest.class);
        log.info("jobConfigChangeRequest = {}", jobConfigChangeRequest);
        jenkinsPluginJobConfigChangeService.processJobConfigChange(company, jobConfigChangeRequest);

        Map<String ,Object> payload = Map.of("run_ids", Collections.emptyList());
        GenericResponse genericResponse = GenericResponse.builder()
                .responseType(responseType)
                .payload(mapper.writeValueAsString(payload))
                .build();
        log.debug("genericResponse = {}", genericResponse);
        return genericResponse;
    }
}
