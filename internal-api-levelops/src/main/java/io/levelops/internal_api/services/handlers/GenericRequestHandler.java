package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.events.models.EventsClientException;
import org.springframework.web.multipart.MultipartFile;

public interface GenericRequestHandler {
    String getRequestType();
    default GenericResponse handleRequest(String company, GenericRequest genericRequest) throws JsonProcessingException, EventsClientException {
        return handleRequest(company, genericRequest, null);
    }
    GenericResponse handleRequest(String company, GenericRequest genericRequest, MultipartFile zipFile) throws JsonProcessingException, EventsClientException;
}
