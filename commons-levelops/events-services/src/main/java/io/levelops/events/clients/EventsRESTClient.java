package io.levelops.events.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Event;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.events.models.EventsClientException;
import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.List;
import java.util.Map;

public class EventsRESTClient implements EventsClient {

    private final ClientHelper<EventsClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    public EventsRESTClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, EventsClientException.class);
    }

    private Builder getBaseUrlBuilder(){
        return HttpUrl.parse(apiBaseUrl).newBuilder()
            .addPathSegment("v1")
            .addPathSegment("events");
    }

    @Override
    public void emitEvent(final String company, final EventType type, final Map<String, Object> eventData) throws EventsClientException {
        HttpUrl url = getBaseUrlBuilder()
            .addPathSegment("emit")
            .build();
        Request request = new Request.Builder()
            .url(url)
            .post(clientHelper.createJsonRequestBody(Event.builder().company(company).type(type).data(eventData).build()))
            .build();
        clientHelper.executeRequest(request);
    }

    @Override
    public List<String> processTriggerEvent(final Event triggerEvent) throws EventsClientException {
        HttpUrl url = getBaseUrlBuilder()
                .addPathSegment("trigger_events")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(triggerEvent))
                .build();
        ProcessTriggerEventsResponse results =  clientHelper.executeAndParse(request, ProcessTriggerEventsResponse.class);
        return results.getRunIds();
    }


    @lombok.Builder
    @Data
    @AllArgsConstructor
    @JsonDeserialize(builder = ProcessTriggerEventsResponse.ProcessTriggerEventsResponseBuilder.class)
    public static class ProcessTriggerEventsResponse{
        @JsonProperty("run_ids")
        private final List<String> runIds;
    }
    
}