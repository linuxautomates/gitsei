package io.levelops.integrations.sonarqube.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

public class SonarQubeIssueDataSourceIntegrationTest {
    @Test
    public void test() throws FetchException {
        // Create a test SonarQubeIssueDataSource
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        InventoryService inventoryService = new InventoryServiceImpl(
                "http://localhost:8080", okHttpClient, objectMapper
        );
        SonarQubeClientFactory factory = new SonarQubeClientFactory(
                inventoryService, objectMapper, okHttpClient,  100
        );
        SonarQubeIssueDataSource sonarQubeIssueDataSource = new SonarQubeIssueDataSource(
                factory
        );
        var issues= sonarQubeIssueDataSource.fetchMany(
                SonarQubeIterativeScanQuery.builder()
                        .integrationKey(IntegrationKey.builder()
                                .tenantId("fabric")
                                .integrationId("8")
                                .build())
                        .projectKeys(Set.of())
                        .usePrivilegedAPIs(false)
//                        .from(Date.from(Instant.now()))
                        .build()
        ).collect(Collectors.toList());
        System.out.println("issues = " + issues);
    }
}
