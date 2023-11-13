package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.events.models.EventsClientException;
import io.levelops.internal_api.services.handlers.GenericRequestHandler;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class GenericRequestHandlerService {
    private final Map<String, GenericRequestHandler> requestHandlers;

    public GenericRequestHandlerService(List<GenericRequestHandler> handlers) {
        requestHandlers = handlers.stream().collect(Collectors.toMap(GenericRequestHandler::getRequestType, h -> h));
    }

    public GenericResponse handleRequest(String company, GenericRequest request, MultipartFile zipFile) throws BadRequestException, JsonProcessingException, EventsClientException {
        if(!requestHandlers.containsKey(request.getRequestType())){
            throw new BadRequestException("Request Type = " + request.getRequestType() + " is not supported!!");
        }
        if (zipFile == null) {
            return requestHandlers.get(request.getRequestType()).handleRequest(company, request);
        } else {
            return requestHandlers.get(request.getRequestType()).handleRequest(company, request, zipFile);
        }
    }
}
