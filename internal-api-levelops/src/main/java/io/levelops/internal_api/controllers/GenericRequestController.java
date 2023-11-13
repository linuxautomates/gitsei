package io.levelops.internal_api.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.internal_api.services.GenericRequestHandlerService;
import io.levelops.web.util.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/generic-requests")
public class GenericRequestController {
    private final GenericRequestHandlerService genericRequestHandlerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public GenericRequestController(GenericRequestHandlerService genericRequestHandlerService, ObjectMapper objectMapper) {
        this.genericRequestHandlerService = genericRequestHandlerService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<GenericResponse>> createGenericRequest(@PathVariable("company") String company,
                                                                                @RequestBody GenericRequest request) {
        return SpringUtils.deferResponse(() -> {
            GenericResponse genericResponse = genericRequestHandlerService.handleRequest(company, request, null);
            return ResponseEntity.accepted().body(genericResponse);
        });
    }

    @PostMapping(path = "/multipart", produces = "application/json")
    public DeferredResult<ResponseEntity<GenericResponse>> createGenericRequestMultiPart(@PathVariable("company") String company,
                                                                                         @RequestPart("json") MultipartFile jsonFile,
                                                                                         @RequestPart(name = "file", required = false) MultipartFile zipFile) {
        return SpringUtils.deferResponse(() -> {
            GenericRequest genericRequest = objectMapper.readValue(jsonFile.getBytes(), GenericRequest.class);
            GenericResponse genericResponse = genericRequestHandlerService.handleRequest(company, genericRequest, zipFile);
            return ResponseEntity.accepted().body(genericResponse);
        });
    }

}
