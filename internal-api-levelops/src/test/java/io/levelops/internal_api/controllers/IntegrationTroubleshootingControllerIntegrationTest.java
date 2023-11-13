package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.junit.Test;

@Log4j2
public class IntegrationTroubleshootingControllerIntegrationTest {

    @Test
    public void test() throws IngestionServiceException, JsonProcessingException {
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        ControlPlaneService controlPlaneService = new ControlPlaneService(okHttpClient, objectMapper, "http://127.0.0.1:8081", true);
        IntegrationTroubleshootingController troubleshootingController = new IntegrationTroubleshootingController(controlPlaneService);

        var result = troubleshootingController.troubleshootIntegrationInternal("sidofficial", "9");
        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }
}