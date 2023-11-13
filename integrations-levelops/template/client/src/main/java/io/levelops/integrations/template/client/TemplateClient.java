package io.levelops.integrations.template.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.ClientHelper.BodyAndHeaders;
import io.levelops.integrations.template.models.TemplateDataQuery;
import io.levelops.integrations.template.models.TemplateResoureceExample;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;

@Log4j2
public class TemplateClient {
    private static final String RESOURCE_URL = "https://api.template.com/resource";
    
    private final ClientHelper<TemplateClientException> clientHelper;

    @Builder
    public TemplateClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper){
        this.clientHelper = ClientHelper.<TemplateClientException>builder()
                                .client(okHttpClient)
                                .objectMapper(objectMapper)
                                .exception(TemplateClientException.class)
                                .build();
    }

    public List<Map<String, Object>> getTemplateResourceExample(TemplateDataQuery query) throws TemplateClientException {
        var url = query.getOffsetLink() != null ? HttpUrl.parse(query.getOffsetLink()).newBuilder()
                : HttpUrl.parse(RESOURCE_URL).newBuilder()
                .addEncodedQueryParameter("admin","false")
                .addEncodedQueryParameter("per_page","100");
        Request request = new Request.Builder()
                            .url(url.build())
                            .get()
                            .build();
        BodyAndHeaders<TemplateResoureceExample> page = clientHelper.executeAndParseWithHeaders(request, TemplateResoureceExample.class);
        // return page.getItems();
        return null;
    }
}