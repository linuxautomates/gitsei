package io.levelops.commons.inventory.adfs;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import okhttp3.OkHttpClient;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class AdfsClientTest {

    @ClassRule
    public static final WireMockClassRule wireMockRule = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Test
    public void test() throws AdfsClientException {
        stubFor(post(urlEqualTo("/"))
                .withRequestBody(equalTo("client_id=cId&resource=rsrc&username=user&password=pwd&grant_type=password"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"token123\"}")));

        AdfsClient adfsClient = new AdfsClient(new OkHttpClient(), wireMockRule.baseUrl(), "cId", "rsrc", "user", "pwd");
        String accessToken = adfsClient.getAccessToken();
        System.out.println(accessToken);
        assertThat(accessToken).isEqualTo("token123");
    }
}