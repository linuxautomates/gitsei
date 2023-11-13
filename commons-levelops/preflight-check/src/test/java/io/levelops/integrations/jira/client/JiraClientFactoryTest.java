package io.levelops.integrations.jira.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.jira.models.JiraMyself;
import okhttp3.OkHttpClient;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraClientFactoryTest {

    @ClassRule
    public static final WireMockClassRule wireMockRule = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Test
    public void testGetSensitiveFields() {
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
        map.put(0, "Description");
        map.put(1, "Summary");
        List<String> fields = JiraClientFactory.getSensitiveFields(Integration.builder()
                .metadata(Map.of("sensitive_fields", map))
                .build());
        assertThat(fields).containsExactlyInAnyOrder("Description", "Summary");

        fields = JiraClientFactory.getSensitiveFields(Integration.builder()
                .metadata(Map.of("sensitive_fields", List.of(" Description ", "  ", "", " Summary ")))
                .build());
        assertThat(fields).containsExactlyInAnyOrder("Description", "Summary");

        fields = JiraClientFactory.getSensitiveFields(Integration.builder()
                .metadata(Map.of("sensitive_fields", "Description , Summary,,"))
                .build());
        assertThat(fields).containsExactlyInAnyOrder("Description", "Summary");
    }

    @Test
    public void testADFS() throws InventoryException, JiraClientException {
        // ADFS first refresh
        stubFor(post(urlEqualTo("/adfs"))
                .withRequestBody(equalTo("client_id=cId&resource=rsrc&username=user&password=pwd&grant_type=password"))
                .inScenario("adfs token refresh")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"token123\"}"))
                .willSetStateTo("refresh1"));

        // ADFS second refresh
        stubFor(post(urlEqualTo("/adfs"))
                .withRequestBody(equalTo("client_id=cId&resource=rsrc&username=user&password=pwd&grant_type=password"))
                .inScenario("adfs token refresh")
                .whenScenarioStateIs("refresh1")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"token456\"}")));

        // jira unauthenticated
        stubFor(get(urlEqualTo("/jira/rest/api/2/myself"))
                .withHeader("Authorization", WireMock.absent())
                .willReturn(WireMock.unauthorized()));

        // jira expired token
        stubFor(get(urlEqualTo("/jira/rest/api/2/myself"))
                .withHeader("Authorization", equalTo("Bearer token123"))
                .willReturn(WireMock.unauthorized()));

        // jira valid token
        stubFor(get(urlEqualTo("/jira/rest/api/2/myself"))
                .withHeader("Authorization", equalTo("Bearer token456"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\": \"maxime\"}")));

        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .adfsOauthToken("test", "1", "jira", wireMockRule.baseUrl() + "/jira", Map.of(),
                        wireMockRule.baseUrl() + "/adfs", "cId", "rsrc", "user", "pwd")
                .build());
        JiraClientFactory jiraClientFactory = new JiraClientFactory(inventoryService, DefaultObjectMapper.get(), new OkHttpClient(), true, null);

        JiraClient client = jiraClientFactory.buildFromInventory(new IntegrationKey("test", "1"));

        JiraMyself output = client.getMyself();
        assertThat(output.getName()).isEqualTo("maxime");
    }
}