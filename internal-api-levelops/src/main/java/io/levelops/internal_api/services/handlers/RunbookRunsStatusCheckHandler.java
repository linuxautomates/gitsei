package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunState;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.databases.services.RunbookRunDatabaseService;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.events.models.EventsClientException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class RunbookRunsStatusCheckHandler implements GenericRequestHandler {
    private final String requestType = "RunbookRunsStatusCheckRequest";
    private final ObjectMapper mapper;
    private final RunbookRunDatabaseService runbookRunDatabaseService;

    @Autowired
    public RunbookRunsStatusCheckHandler(ObjectMapper mapper, RunbookRunDatabaseService runbookRunDatabaseService) {
        this.mapper = mapper;
        this.runbookRunDatabaseService = runbookRunDatabaseService;
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

        RunbookRunsStatusCheckRequest request = mapper.readValue(genericRequest.getPayload(), RunbookRunsStatusCheckRequest.class);
        log.debug("request = {}", request);

        List<RunbookRunStatusCheckResponse> responses = new ArrayList<>();
        for(String runId : request.getRunIds()){
            Optional<RunbookRun> optionalRunbookRun = null;
            try {
                optionalRunbookRun = runbookRunDatabaseService.get(company, runId);
            } catch (SQLException e) {
                log.error("SQLException!!", e);
            }
            if(optionalRunbookRun.isEmpty()){
                continue;
            }
            RunbookRun runbookRun = optionalRunbookRun.get();
            RunbookRunStatusCheckResponse.RunbookRunStatusCheckResponseBuilder bldr = RunbookRunStatusCheckResponse.builder()
                    .runId(runbookRun.getId())
                    .state(runbookRun.getState());
            if(runbookRun.getResult() != null){
                bldr.output(runbookRun.getResult().getOutput());
            }
            responses.add(bldr.build());
        }
        GenericResponse genericResponse = GenericResponse.builder()
                .responseType("RunbookRunsStatusCheckResponse")
                .payload(mapper.writeValueAsString(responses))
                .build();
        log.debug("genericResponse = {}", genericResponse);
        return genericResponse;
    }

    @Builder(toBuilder = true)
    @Data
    @JsonDeserialize(builder = RunbookRunsStatusCheckRequest.RunbookRunsStatusCheckRequestBuilder.class)
    public static class RunbookRunsStatusCheckRequest {
        @JsonProperty("run_ids")
        private final List<String> runIds;
    }

    @Builder(toBuilder = true)
    @Data
    @JsonDeserialize(builder = RunbookRunStatusCheckResponse.RunbookRunStatusCheckResponseBuilder.class)
    public static class RunbookRunStatusCheckResponse {
        @JsonProperty("run_id")
        private final String runId;
        @JsonProperty("state")
        private final RunbookRunState state;
        @JsonProperty("output")
        Map<String, RunbookVariable> output;
    }
}
