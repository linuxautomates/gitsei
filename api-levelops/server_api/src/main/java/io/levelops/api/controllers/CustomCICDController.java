package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.jenkins.JobRunCompleteRequest;
import io.levelops.commons.generic.clients.GenericRequestsClient;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

@RestController
@Log4j2
@RequestMapping("/v1/custom-cicd")
public class CustomCICDController {
    private final GenericRequestsClient genericRequestsClient;

    private final ObjectMapper mapper;

    private static String FIELD_IS_MISSING = " field is missing in request";

    @Autowired
    public CustomCICDController(GenericRequestsClient genericRequestsClient,
                                ObjectMapper mapper){
        this.genericRequestsClient = genericRequestsClient;
        this.mapper = mapper;
    }

    @PostMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<GenericResponse>> createGenericRequest(@SessionAttribute("company") final String company,
                                                                                @RequestBody JobRunCompleteRequest request) {

        return SpringUtils.deferResponse(() -> {
            GenericRequest genericRequest = null;
            try {
                genericRequest = parseAndValidateCICDFields(request);
            } catch(GenericRequestsMissingFieldException e) {
                GenericResponse genericResponse = GenericResponse.builder().payload(e.getMessage()).build();
                return ResponseEntity.badRequest().body(genericResponse);
            }
            GenericResponse genericResponse = genericRequestsClient.create(company, genericRequest);
            return ResponseEntity.accepted().body(genericResponse);
        });
    }

    private GenericRequest parseAndValidateCICDFields(JobRunCompleteRequest jobRunCompleteRequest) throws IOException, GenericRequestsMissingFieldException {

        if (StringUtils.isEmpty(jobRunCompleteRequest.getJobName())) {
            throw new GenericRequestsMissingFieldException("job_name/pipeline" + FIELD_IS_MISSING);
        }

        if (StringUtils.isEmpty(jobRunCompleteRequest.getJenkinsInstanceName())) {
            throw new GenericRequestsMissingFieldException("jenkins_instance_name/instance_name" + FIELD_IS_MISSING);
        }

        if (StringUtils.isEmpty(jobRunCompleteRequest.getJenkinsInstanceGuid())) {
            throw new GenericRequestsMissingFieldException("jenkins_instance_guid/instance_guid" + FIELD_IS_MISSING);
        }

        if (StringUtils.isEmpty(jobRunCompleteRequest.getJobFullName())) {
            throw new GenericRequestsMissingFieldException("job_full_name" + FIELD_IS_MISSING);
        }

        if (StringUtils.isEmpty(jobRunCompleteRequest.getJobNormalizedFullName())) {
            throw new GenericRequestsMissingFieldException("job_normalized_full_name/qualified_name" + FIELD_IS_MISSING);
        }

        if (StringUtils.isEmpty(jobRunCompleteRequest.getResult())) {
            throw new GenericRequestsMissingFieldException("result" + FIELD_IS_MISSING);
        }

        return GenericRequest.builder().payload(mapper.writeValueAsString(jobRunCompleteRequest)).requestType("JenkinsPluginJobRunComplete").build();
    }

    public static class GenericRequestsMissingFieldException extends Exception {
        public GenericRequestsMissingFieldException(String message) {
            super(message);
        }
    }
}
