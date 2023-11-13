package io.propelo.trellis_framework.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.models.ListResponse;
import io.propelo.trellis_framework.client.exception.TrellisControllerClientException;
import io.propelo.trellis_framework.models.audit.AuditLogEntry;
import io.propelo.trellis_framework.models.events.Event;
import io.propelo.trellis_framework.models.events.EventResult;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.List;
import java.util.Optional;

@Log4j2
public class TrellisAPIControllerClient {
    private final ClientHelper<TrellisControllerClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String trellisControllerServiceUri;

    @Builder
    public TrellisAPIControllerClient(final OkHttpClient client, final ObjectMapper objectMapper, final String trellisControllerServiceUri) {
        this.objectMapper = objectMapper;
        this.trellisControllerServiceUri = trellisControllerServiceUri;
        this.clientHelper = new ClientHelper<>(client, objectMapper, TrellisControllerClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder() {
        return HttpUrl.parse(trellisControllerServiceUri).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("trellis");
    }

    //region events

    public Id createEvent(Event event) throws TrellisControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment("events");

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(event))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    //end region

    private HttpUrl.Builder getAuditLogsUrlBuilder() {
        return HttpUrl.parse(trellisControllerServiceUri).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("audit_logs");
    }

    public Id createAuditLogEntry(AuditLogEntry auditLogEntry) throws TrellisControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder();

        HttpUrl url = getAuditLogsUrlBuilder().build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(auditLogEntry))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public Optional<Id> createAuditLogEntrySafe(AuditLogEntry auditLogEntry) {
        try {
            return Optional.ofNullable(createAuditLogEntry(auditLogEntry));
        } catch (TrellisControllerClientException e) {
            log.error("Error creating audit log entry! " + auditLogEntry.toString(), e);
            return Optional.empty();
        }
    }
}
