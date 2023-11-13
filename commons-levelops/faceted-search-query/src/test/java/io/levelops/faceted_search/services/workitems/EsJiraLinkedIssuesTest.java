package io.levelops.faceted_search.services.workitems;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.faceted_search.services.scm_service.EsTestUtils;
import lombok.SneakyThrows;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import  org.assertj.core.api.Assertions;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class EsJiraLinkedIssuesTest {

    private static String company = "test";
    private static ESContext esContext;
    private static EsTestUtils esTestUtils = new EsTestUtils();
    private static ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static String index = "work_items_test_1691452800";
    private static EsJiraIssueQueryService esJiraIssueQueryService;
    private static EsJiraDBHelperService esJiraDBHelperService;

    @BeforeClass
    public static void startESCreateClient() throws IOException, InterruptedException {
        esContext = esTestUtils.initializeESClient();
        createIndex();
        insertData();

        esJiraDBHelperService = mock(EsJiraDBHelperService.class);
        Mockito.when(esJiraDBHelperService.getDbJiraFields(any(), any(), any())).thenReturn(List.of());

        ESClientFactory esClientFactory = EsTestUtils.buildESClientFactory(esContext);
        esJiraIssueQueryService = new EsJiraIssueQueryService(esClientFactory, esJiraDBHelperService);
    }

    @Test
    public void testDependencyFilter() throws SQLException, IOException {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(1691452800l)
                .links(List.of("relates to"))
                .projects(List.of("LEV"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .build();

        DbListResponse<DbAggregationResult> res =  esJiraIssueQueryService.getAggReport(company, jiraIssuesFilter, List.of(), null, 0, 10, false);

        Assert.assertNotNull(res);
        List<DbAggregationResult>result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("LEV");
        Assertions.assertThat(result.get(0).getTotalTickets()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getTotalStoryPoints()).isEqualTo(0);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("LFE");
        Assertions.assertThat(result.get(1).getTotalTickets()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getTotalStoryPoints()).isEqualTo(0);

        Assertions.assertThat(result.get(2).getKey()).isEqualTo("PROP");
        Assertions.assertThat(result.get(2).getTotalTickets()).isEqualTo(1);
        Assertions.assertThat(result.get(2).getTotalStoryPoints()).isEqualTo(2);

        jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(1691452800l)
                .links(List.of("relates to"))
                .statuses(List.of("DONE"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .build();

        res =  esJiraIssueQueryService.getAggReport(company, jiraIssuesFilter, List.of(), null, 0, 10, false);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("LEV");
        Assertions.assertThat(result.get(0).getTotalTickets()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getTotalStoryPoints()).isEqualTo(0);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("LFE");
        Assertions.assertThat(result.get(1).getTotalTickets()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getTotalStoryPoints()).isEqualTo(0);

        Assertions.assertThat(result.get(2).getKey()).isEqualTo("PROP");
        Assertions.assertThat(result.get(2).getTotalTickets()).isEqualTo(1);
        Assertions.assertThat(result.get(2).getTotalStoryPoints()).isEqualTo(2);

        jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(1691452800l)
                .links(List.of("relates to"))
                .statuses(List.of("DONE"))
                .across(JiraIssuesFilter.DISTINCT.assignee)
                .acrossLimit(20)
                .build();

        res =  esJiraIssueQueryService.getAggReport(company, jiraIssuesFilter, List.of(), null, 0, 10, false);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(6);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("Maxime Bellier");
        Assertions.assertThat(result.get(0).getTotalTickets()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getTotalStoryPoints()).isEqualTo(0);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("Ajay Sakat");
        Assertions.assertThat(result.get(1).getTotalTickets()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getTotalStoryPoints()).isEqualTo(0);

        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("Gopal Mandloi");
        Assertions.assertThat(result.get(2).getTotalTickets()).isEqualTo(1);
        Assertions.assertThat(result.get(2).getTotalStoryPoints()).isEqualTo(2);

        Assertions.assertThat(result.get(3).getAdditionalKey()).isEqualTo("Shivam Yadav");
        Assertions.assertThat(result.get(3).getTotalTickets()).isEqualTo(1);
        Assertions.assertThat(result.get(3).getTotalStoryPoints()).isEqualTo(0);
    }


    private static void insertData() throws IOException {

        String data = ResourceUtils.getResourceAsString("data/jira_issues.json");
        List<EsWorkItem> allWorkitems = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, EsWorkItem.class));

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (EsWorkItem wi : allWorkitems) {
            BulkOperation.Builder b = new BulkOperation.Builder();
            b.update(v -> v
                    .index(index)
                    .id(wi.getId().toString())
                    .action(a -> a
                            .docAsUpsert(true)
                            .doc(wi)
                    )
            );
            bulkOperations.add(b.build());
        }
        BulkRequest.Builder bldr = new BulkRequest.Builder();
        bldr.operations(bulkOperations);
        esContext.getClient().bulk(bldr.build());
    }

    @Test
    public void testDependencyFilterList() throws SQLException, IOException {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .ingestedAt(1691452800l)
                .links(List.of("relates to"))
                .projects(List.of("LEV"))
                .across(JiraIssuesFilter.DISTINCT.project)
                .build();
        JiraIssuesFilter jiraIssueLinkedsFilter = JiraIssuesFilter.builder()
                .ingestedAt(1691452800l)
                .projects(List.of("LFE"))
                .build();

        DbListResponse<DbJiraIssue> res = esJiraIssueQueryService.getJiraIssuesList(company, jiraIssuesFilter, jiraIssueLinkedsFilter, null, 0, 10, Optional.empty());
        Assert.assertNotNull(res);
        List<DbJiraIssue> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("LFE-3098");
        Assertions.assertThat(result.get(1).getKey()).isEqualTo("LFE-3077");
        Assertions.assertThat(result.get(2).getKey()).isEqualTo("LFE-2998");
    }

    @SneakyThrows
    private static void createIndex() {
        String indexTemplate = ResourceUtils.getResourceAsString("index/workitem_index_template.json");
        esContext.getClient().indices().create(new CreateIndexRequest.Builder()
                .index(index)
                .aliases(index + "_a", new Alias.Builder().build())
                .settings(new IndexSettings.Builder().numberOfShards("4").codec("best_compression").build())
                .mappings(TypeMapping.of(s -> s.withJson(new StringReader(indexTemplate))))
                .build());
    }

    @AfterClass
    public static void closeResources() throws Exception {
        esTestUtils.deleteIndex(index);
        esTestUtils.closeResources();
    }
}
