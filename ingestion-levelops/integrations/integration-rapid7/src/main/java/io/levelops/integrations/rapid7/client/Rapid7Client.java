package io.levelops.integrations.rapid7.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.rapid7.models.api.Rapid7ApiApp;
import io.levelops.integrations.rapid7.models.api.Rapid7ApiModule;
import io.levelops.integrations.rapid7.models.api.Rapid7ApiPaginatedResponse;
import io.levelops.integrations.rapid7.models.api.Rapid7ApiVulnerability;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.Validate;

import java.util.Set;
import java.util.function.Supplier;

public class Rapid7Client {

    /*
      Possible URLS for InsightAppSec:
        United States: https://us.api.insight.rapid7.com/ias
        Europe: https://eu.api.insight.rapid7.com/ias
        Canada: https://ca.api.insight.rapid7.com/ias
        Australia: https://au.api.insight.rapid7.com/ias
        Japan: https://ap.api.insight.rapid7.com/ias

      Example: curl -H "X-Api-Key: API_KEY" https://us.api.insight.rapid7.com/validate
     */

    public static final Set<String> REGIONS = Set.of("us", "eu", "ca", "au", "ap");
    private static final String BASE_URL_FORMAT = "https://%s.api.insight.rapid7.com/ias/v1/"; // <region>
    private final ObjectMapper objectMapper;
    private final Supplier<String> region;
    private final ClientHelper<Rapid7ClientException> clientHelper;

    @Builder
    public Rapid7Client(OkHttpClient okHttpClient, ObjectMapper objectMapper, Supplier<String> region) {
        Validate.isTrue(REGIONS.contains(region), "Unsupported region: " + region);
        this.objectMapper = objectMapper;
        this.region = region;
        clientHelper = ClientHelper.<Rapid7ClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(Rapid7ClientException.class)
                .build();
    }

    //  https://[region].api.insight.rapid7.com/ias/v1/vulnerabilities
    private HttpUrl.Builder baseUrlBuilder() {
        String currentRegion = region.get();
        return HttpUrl.parse(String.format(BASE_URL_FORMAT, currentRegion)).newBuilder();
    }

    // https://help.rapid7.com/insightappsec/en-us/api/v1/docs.html#operation/get-apps
    public Rapid7ApiPaginatedResponse<Rapid7ApiApp> getApps(int pageNumber) throws Rapid7ClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("apps")
                .addQueryParameter("index", String.valueOf(pageNumber))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(
                        Rapid7ApiPaginatedResponse.class,
                        Rapid7ApiApp.class));
    }

    public Rapid7ApiApp getApp(String appId) throws Rapid7ClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("apps")
                .addPathSegment(appId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, Rapid7ApiApp.class);
    }

    // https://help.rapid7.com/insightappsec/en-us/api/v1/docs.html#tag/Vulnerabilities
    public Rapid7ApiPaginatedResponse<Rapid7ApiVulnerability> getVulnerabilities(int pageNumber) throws Rapid7ClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("vulnerabilities")
                .addQueryParameter("index", String.valueOf(pageNumber))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(
                        Rapid7ApiPaginatedResponse.class,
                        Rapid7ApiVulnerability.class));
    }

    public Rapid7ApiModule getModule(String moduleId) throws Rapid7ClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("modules")
                .addPathSegment(moduleId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, Rapid7ApiModule.class);
    }
}
