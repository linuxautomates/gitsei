package io.levelops.controlplane.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Log4j2
@Service
public class TriggerCallbackService {

    private final ClientHelper<CallbackException> clientHelper;

    @Autowired
    public TriggerCallbackService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        clientHelper = ClientHelper.<CallbackException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(CallbackException.class)
                .build();
    }

    public void callback(DbTrigger trigger, TriggerResults results) {
        log.debug("↪︎ Calling back for trigger_id={} at {}", trigger.getId(), trigger.getCallbackUrl());
        try {
            String callbackUrl = trigger.getCallbackUrl();
            String response = doCallBack(callbackUrl, results);
            log.info("↪︎ Callback successful for trigger_id={} at {} (response='{}')", trigger.getId(), trigger.getCallbackUrl(), response);
        } catch (CallbackException e) {
            log.warn("↪︎ Callback failed for trigger_id={} at {}", trigger, trigger.getCallbackUrl(), e);
        }
    }


    @Nullable
    private String doCallBack(String callbackUrl, TriggerResults results) throws CallbackException {
        if (Strings.isEmpty(callbackUrl)) {
            return null;
        }

        Request request = new Request.Builder()
                .url(HttpUrl.parse(callbackUrl))
                .post(clientHelper.createJsonRequestBody(results))
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
