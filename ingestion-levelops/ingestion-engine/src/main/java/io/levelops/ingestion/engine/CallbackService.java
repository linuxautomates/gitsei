package io.levelops.ingestion.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.models.ExceptionPrintout;
import io.levelops.ingestion.engine.models.JobConverter;
import io.levelops.ingestion.models.Job;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;

@Log4j2
public class CallbackService {

    private final ClientHelper<CallbackException> clientHelper;

    public CallbackService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        clientHelper = ClientHelper.<CallbackException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(CallbackException.class)
                .build();
    }

    public void callback(IngestionEngine.EngineJob job) {
        log.info("↪︎ Calling back for job={} at {}", job.getId(), job.getCallbackUrl());
        try {
            String callbackUrl = job.getCallbackUrl();
            String response = doCallBack(callbackUrl, JobConverter.fromEngineJob(job));
            log.info("↪︎ Callback successful for job={}: {}", job.getId(), response);
        } catch (CallbackException e) {
            log.warn("↪︎ Callback failed for job={}", job, e);
        }
    }


    @Nullable
    private String doCallBack(String callbackUrl, Job job) throws CallbackException {
        if (Strings.isEmpty(callbackUrl)) {
            return null;
        }

        Request request = new Request.Builder()
                .url(HttpUrl.parse(callbackUrl))
                .post(clientHelper.createJsonRequestBody(job))
                .build();
        return clientHelper.executeRequest(request);
    }

    public static class CallbackException extends Exception {
        public CallbackException() {
        }

        public CallbackException(String message) {
            super(message);
        }

        public CallbackException(String message, Throwable cause) {
            super(message, cause);
        }

        public CallbackException(Throwable cause) {
            super(cause);
        }
    }
}
