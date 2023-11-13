package io.levelops.integrations.jira.sources;

import java.net.URISyntaxException;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraProject;
import okhttp3.OkHttpClient;

public class JiraDataSourceIntegrationTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(JiraDataSourceIntegrationTest.class);
    private JiraClientFactory jiraClientFactory;

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        jiraClientFactory = JiraClientFactory.builder()
                .inventoryService(new InventoryServiceImpl("http://localhost:9999", okHttpClient, DefaultObjectMapper.get()))
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .build();

    }

    @Test
    public void getUser() throws JiraClientException {
//        User claim = jiraClientFactory.get(IntegrationKey.builder()
//                .integrationId("jira")
//                .tenantId("coke")
//                .build()).getUserClient().getUser("admin").claim();
//        System.out.println(claim);
    }

    @Test
    public void projects() throws URISyntaxException, FetchException {

        JiraProjectDataSource jiraDataSource = new JiraProjectDataSource(jiraClientFactory);
        jiraDataSource.fetchMany(JiraProjectDataSource.JiraProjectQuery.builder()
                .integrationKey(new IntegrationKey("coke", "jira"))
                .build())
                .forEach(System.out::println);

        Data<JiraProject> jiraProjectData = jiraDataSource.fetchOne(JiraProjectDataSource.JiraProjectQuery.builder()
                .integrationKey(new IntegrationKey("coke", "jira"))
                .projectKey("LEV")
                .build());
        DefaultObjectMapper.prettyPrint(jiraProjectData);
    }

    @Test
    public void issues() throws URISyntaxException, FetchException {

        JiraIssueDataSource jiraDataSource = new JiraIssueDataSource(jiraClientFactory);
        jiraDataSource.fetchMany(JiraIssueDataSource.JiraIssueQuery.builder()
                .integrationKey(new IntegrationKey("coke", "jira"))
//                .jql("updated <= '2019-11-14 13:49'")
                .jql("updated >= '2019-11-14' and key = 'LEV-63'")
                .limit(1000)
                .build())
                .peek(i -> DefaultObjectMapper.prettyPrint(i.getPayload()))
                .peek(i -> System.out.println(i.getPayload().getKey()))
                .filter(i -> !i.getPayload().getFields().getLabels().isEmpty())
//                .count();
                .peek(i -> System.out.println(i.getPayload().getFields().getLabels()))
                .peek(i -> System.out.println(i.getPayload().getFields().getUpdated()))
                .map(i -> i.getPayload().getKey() + " - " + i.getPayload().getFields().getSummary())
                .forEach(System.out::println);
    }
}