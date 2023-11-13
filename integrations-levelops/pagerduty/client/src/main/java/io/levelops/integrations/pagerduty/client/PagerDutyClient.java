package io.levelops.integrations.pagerduty.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.pagerduty.models.PagerDutyAlertsPage;
import io.levelops.integrations.pagerduty.models.PagerDutyDataQuery;
import io.levelops.integrations.pagerduty.models.PagerDutyIncidentsPage;
import io.levelops.integrations.pagerduty.models.PagerDutyLogEntriesPage;
import io.levelops.integrations.pagerduty.models.PagerDutyServicesPage;
import io.levelops.integrations.pagerduty.models.PagerDutyUsersPage;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;

@Log4j2
public class PagerDutyClient{
    private static final String PAGER_DUTY_BASE_URL = "https://api.pagerduty.com";
    // private static final String SERVICES_URL = PAGER_DUTY_BASE_URL + "/services";
    private static final String INCIDENTS_URL = PAGER_DUTY_BASE_URL + "/incidents";
    private static final String ALERTS_URL = PAGER_DUTY_BASE_URL + "/alerts";
    private static final String LOG_ENTRY_URL = PAGER_DUTY_BASE_URL + "/log_entries";
    private static final String SERVICES_URL = PAGER_DUTY_BASE_URL + "/services";
    private static final String USERS_URL = PAGER_DUTY_BASE_URL + "/users";

    private final ClientHelper<PagerDutyClientException> clientHelper;

    @Builder
    public PagerDutyClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper){
        this.clientHelper = ClientHelper.<PagerDutyClientException>builder()
                                .client(okHttpClient)
                                .objectMapper(objectMapper)
                                .exception(PagerDutyClientException.class)
                                .build();
    }
    
    public <T> List<Map<String, Object>> getPagerDutyItems(PagerDutyDataQuery query, Class<T> claz) throws PagerDutyClientException {
        switch(claz.getSimpleName()){
            case "PagerDutyAlertsPage": return getPagerDutyAlerts((PagerDutyAlertsPage.Query) query);
            case "PagerDutyIncidentsPage": return getPagerDutyIncidents((PagerDutyIncidentsPage.Query) query);
            case "PagerDutyLogEntriesPage": return getPagerDutyLogEntries((PagerDutyLogEntriesPage.Query) query);
            case "PagerDutyServicesPage": return getPagerDutyServices((PagerDutyServicesPage.Query) query);
            case "PagerDutyUsersPage": return getPagerDutyUsers((PagerDutyUsersPage.Query) query);
        };
        throw new PagerDutyClientException("Unsupported pager duty type: " + claz.getSimpleName());
    }
    
    public List<Map<String, Object>> getPagerDutyAlerts(PagerDutyAlertsPage.Query query) throws PagerDutyClientException {
        Validate.notNull(query.getLimit(), "Limit is mandatory in a query object");
        Validate.notNull(query.getOffset(), "Offset is mandatory in a query object");
        var url = HttpUrl.parse(ALERTS_URL).newBuilder()
                .addEncodedQueryParameter("time_zone","UTC")
                .addEncodedQueryParameter("include[]","incidents")
                .addEncodedQueryParameter("offset", String.valueOf(query.getOffset()))
                .addEncodedQueryParameter("limit", String.valueOf(query.getLimit()));
        if(query.getSince() != null){
            url.addEncodedQueryParameter("since", query.getSince());
        }
        if(query.getUntil() != null){
            url.addEncodedQueryParameter("until", query.getUntil());
        }
        Request request = new Request.Builder()
                            .url(url.build())
                            .header(ClientConstants.ACCEPT, "application/vnd.pagerduty+json;version=2")
                            .get()
                            .build();
        PagerDutyAlertsPage page = clientHelper.executeAndParse(request, PagerDutyAlertsPage.class);
        return page.getItems();
    }
    
    public List<Map<String, Object>> getPagerDutyIncidents(PagerDutyIncidentsPage.Query query) throws PagerDutyClientException {
        Validate.notNull(query.getLimit(), "Limit is mandatory in a query object");
        Validate.notNull(query.getOffset(), "Offset is mandatory in a query object");
        var url = HttpUrl.parse(INCIDENTS_URL).newBuilder()
                .addEncodedQueryParameter("time_zone","UTC")
                .addEncodedQueryParameter("offset", String.valueOf(query.getOffset()))
                .addEncodedQueryParameter("limit", String.valueOf(query.getLimit()))
                .addEncodedQueryParameter("sort_by", "created_at");
        if(query.getSince() != null){
            url.addEncodedQueryParameter("since", query.getSince());
        }
        if(query.getUntil() != null){
            url.addEncodedQueryParameter("until", query.getUntil());
        }
        if(Strings.isBlank(query.getIncidentKey()) && Strings.isBlank(query.getItemId())){
            if(!CollectionUtils.isEmpty(query.getStatuses())){
                for (String status:query.getStatuses()){
                    url.addEncodedQueryParameter("statuses[]", status);
                }
            }
            if(!CollectionUtils.isEmpty(query.getServiceIds())){
                for (String serviceId:query.getServiceIds()){
                    url.addEncodedQueryParameter("service_ids[]", serviceId);
                }
            }
        }
        else if(!Strings.isBlank(query.getIncidentKey())){
            url.addEncodedQueryParameter("incident_key", query.getIncidentKey());
        }
        else{
            url.addPathSegment(query.getItemId());
        }
        Request request = new Request.Builder()
                            .url(url.build())
                            .header(ClientConstants.ACCEPT, "application/vnd.pagerduty+json;version=2")
                            .get()
                            .build();
        String response = clientHelper.executeRequest(request);
        PagerDutyIncidentsPage page = clientHelper.parseResponse(response, PagerDutyIncidentsPage.class);
        return page.getItems();
    }
    
    public List<Map<String, Object>> getPagerDutyLogEntries(PagerDutyLogEntriesPage.Query query) throws PagerDutyClientException {
        Validate.notNull(query.getLimit(), "Limit is mandatory in a query object");
        Validate.notNull(query.getOffset(), "Offset is mandatory in a query object");
        var url = HttpUrl.parse(LOG_ENTRY_URL).newBuilder()
                .addEncodedQueryParameter("time_zone","UTC")
                .addEncodedQueryParameter("include[]","incidents")
                .addEncodedQueryParameter("offset", String.valueOf(query.getOffset()))
                .addEncodedQueryParameter("limit", String.valueOf(query.getLimit()));
        if(!Strings.isBlank(query.getSince())){
            url.addEncodedQueryParameter("since", query.getSince());
        }
        if(!Strings.isBlank(query.getUntil())){
            url.addEncodedQueryParameter("until", query.getUntil());
        }
        log.debug("PagerDuty url: {}", url);
        Request request = new Request.Builder()
                            .url(url.build())
                            .header(ClientConstants.ACCEPT, "application/vnd.pagerduty+json;version=2")
                            .get()
                            .build();
        String response = clientHelper.executeRequest(request);
        PagerDutyLogEntriesPage page = clientHelper.parseResponse(response, PagerDutyLogEntriesPage.class);
        return page.getItems();
    }
    
    public List<Map<String, Object>> getPagerDutyServices(PagerDutyServicesPage.Query query) throws PagerDutyClientException {
        Validate.notNull(query.getLimit(), "Limit is mandatory in a query object");
        Validate.notNull(query.getOffset(), "Offset is mandatory in a query object");
        var url = HttpUrl.parse(SERVICES_URL).newBuilder()
                .addEncodedQueryParameter("time_zone","UTC")
                .addEncodedQueryParameter("offset", String.valueOf(query.getOffset()))
                .addEncodedQueryParameter("limit", String.valueOf(query.getLimit()));
        log.debug("PagerDuty url: {}", url);
        Request request = new Request.Builder()
                            .url(url.build())
                            .header(ClientConstants.ACCEPT, "application/vnd.pagerduty+json;version=2")
                            .get()
                            .build();
        String response = clientHelper.executeRequest(request);
        PagerDutyServicesPage page = clientHelper.parseResponse(response, PagerDutyServicesPage.class);
        return page.getItems();
    }

    public List<Map<String, Object>> getPagerDutyUsers(PagerDutyUsersPage.Query query) throws PagerDutyClientException {
        var url = HttpUrl.parse(USERS_URL).newBuilder()
                .addEncodedQueryParameter("time_zone","UTC")
                .addEncodedQueryParameter("offset", String.valueOf(query.getOffset()))
                .addEncodedQueryParameter("limit", String.valueOf(query.getLimit()));
        log.debug("PagerDuty url: {}", url);
        Request request = new Request.Builder()
                            .url(url.build())
                            .header(ClientConstants.ACCEPT, "application/vnd.pagerduty+json;version=2")
                            .get()
                            .build();
        String response = clientHelper.executeRequest(request);
        PagerDutyUsersPage page = clientHelper.parseResponse(response, PagerDutyUsersPage.class);
        return page.getItems();
    }
}