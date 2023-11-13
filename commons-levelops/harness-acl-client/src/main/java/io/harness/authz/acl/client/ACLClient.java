package io.harness.authz.acl.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.AccessCheckRequestDTO;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Log4j2
@Data
public class ACLClient {

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final String aclUrl;
    private final ClientHelper<ACLClientException> clientHelper;

    @Builder
    public ACLClient(ObjectMapper objectMapper, OkHttpClient okHttpClient, String aclUrl) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.aclUrl = aclUrl;
        this.clientHelper = ClientHelper.<ACLClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(ACLClientException.class)
                .build();
    }

    private HttpUrl.Builder baseACLUrlBuilder() {
        return HttpUrl.parse(aclUrl).newBuilder()
                .addPathSegment("gateway")
                .addPathSegment("authz")
                .addPathSegment("api")
                .addPathSegment("acl");
    }

    public AccessCheckResponseDTO checkAccess(AccessCheckRequestDTO accessCheckRequestDTO) throws ACLClientException {

        HttpUrl url = HttpUrl.parse(aclUrl).newBuilder().build();

        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.CONTENT_TYPE, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(accessCheckRequestDTO))
                .build();

        return clientHelper.executeAndParse(request, AccessCheckResponseDTO.class);
    }
}
