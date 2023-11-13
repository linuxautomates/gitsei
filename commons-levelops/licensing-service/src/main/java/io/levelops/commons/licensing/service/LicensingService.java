package io.levelops.commons.licensing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.licensing.exception.LicensingException;
import io.levelops.commons.licensing.model.License;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LicensingService {

    private final String licensingServiceUrl;
    private final ObjectMapper objectMapper;
    private final ClientHelper<LicensingException> clientHelper;

    @Builder
    public LicensingService(String licensingServiceUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.licensingServiceUrl = licensingServiceUrl;
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<LicensingException>builder()
                .client(client)
                .objectMapper(objectMapper)
                .exception(LicensingException.class)
                .build();
    }


    public License getLicense(String company) throws LicensingException {

        var url = HttpUrl.parse(licensingServiceUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("licensing")
                .addPathSegment(company)
                .build();

        var request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, License.class);
    }

    /**
     * This will overwrite existing entitlements, it will NOT append.
     * @param company
     * @param entitlements
     * @return New License Object with updated entitlements
     * @throws LicensingException
     */
    public License updateEntitlements(String company, List<String> entitlements) throws LicensingException {
        var url = HttpUrl.parse(licensingServiceUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("licensing")
                .addPathSegment(company)
                .addPathSegment("custom_entitlements")
                .build();

        var request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(Map.of("entitlements", entitlements)))
                .build();

        return clientHelper.executeAndParse(request, License.class);
    }

    /**
     * Not transactional. If another thread/process runns append with different values the last one would persist.
     * @param company
     * @param entitlements
     * @return
     */
    public License appendEntitlements(String company, List<String> entitlements) throws LicensingException {
        License current = getLicense(company);
        List<String> combinedList = new ArrayList<>(current.getEntitlements());
        combinedList.addAll(entitlements);
        License updated = updateEntitlements(company, combinedList.stream().distinct().collect(Collectors.toList()));
        return updated;
    }
}
