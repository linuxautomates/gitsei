package io.levelops.triggers.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.TriggerSchema;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.HttpUrl.Builder;

import java.util.List;
import java.util.Optional;

public class TriggersRESTClient {

    private final ClientHelper<TriggersRESTClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    public TriggersRESTClient(OkHttpClient client, ObjectMapper objectMapper, String triggersApiBaseUrl) {
        this.objectMapper = objectMapper;
        this.internalApiUri = triggersApiBaseUrl;
        clientHelper = new ClientHelper<>(client, objectMapper, TriggersRESTClientException.class);
    }

    private Builder getBaseUrl(final String company) {
            return HttpUrl.parse(internalApiUri).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company);
    }
    
    public Optional<TriggerSchema> getTriggerSchemas(final String company, final TriggerType triggerType)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("triggers")
                .addPathSegment("playbooks")
                .addPathSegment("schemas")
                .addPathSegment(triggerType.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return Optional.of(clientHelper.executeAndParse(request, TriggerSchema.class));
    }
    
    public PaginatedResponse<TriggerSchema> listTriggerSchemas(final String company, final DefaultListRequest searchRequest)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("triggers")
                .addPathSegment("playbooks")
                .addPathSegment("schemas")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(searchRequest))
                .build();
        return clientHelper.executeAndParse(request,objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, TriggerSchema.class));
    }
    
    public List<TriggerType> getTriggerTypes(final String company)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("triggers")
                .addPathSegment("playbooks")
                .addPathSegment("types")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request,objectMapper.getTypeFactory().constructParametricType(List.class, TriggerType.class));
    }

    public Optional<EventType> getEventType(final String company, final EventType eventType)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("events")
                .addPathSegment("types")
                .addPathSegment(eventType.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return Optional.of(clientHelper.executeAndParse(request, EventType.class));
    }
    
    public PaginatedResponse<EventType> listEventTypes(final String company, final DefaultListRequest searchRequest)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("events")
                .addPathSegment("types")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(searchRequest))
                .build();
        return clientHelper.executeAndParse(request,objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, EventType.class));
    }
    
    public Optional<Component> getComponent(final String company, final ComponentType componentType, final String name)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("components")
                .addPathSegment("types")
                .addPathSegment(componentType.toString())
                .addPathSegment(name)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return Optional.of(clientHelper.executeAndParse(request, Component.class));
    }
    
    public List<Component> getComponentsByType(final String company, final ComponentType componentType)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("components")
                .addPathSegment("types")
                .addPathSegment(componentType.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request,objectMapper.getTypeFactory().constructParametricType(List.class, Component.class));
    }
    
    public PaginatedResponse<Component> listComponents(final String company, final DefaultListRequest searchRequest)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("components")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(searchRequest))
                .build();
        return clientHelper.executeAndParse(request,objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, TriggerType.class));
    }
    
    public List<ComponentType> getComponentTypes(final String company)
            throws TriggersRESTClientException {
        HttpUrl url = getBaseUrl(company)
                .addPathSegment("components")
                .addPathSegment("types")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request,objectMapper.getTypeFactory().constructParametricType(List.class, ComponentType.class));
    }
}