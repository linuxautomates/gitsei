package io.levelops.commons.services.business_alignment.es.services;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.services.business_alignment.es.query_builder.BaJiraWIAggsESQueryBuilder;
import io.levelops.commons.services.business_alignment.es.result_converter.composite.BACompositeESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.result_converter.terms.BATermsESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.utils.EsTestUtils;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.faceted_search.services.workitems.EsJiraDBHelperService;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class BaJiraAggsESServiceTest {
    private static String company = "test";
    private static ESContext esContext;
    private static EsTestUtils esTestUtils = new EsTestUtils();
    private static ObjectMapper M = DefaultObjectMapper.get();
    private static String index = "work_items_test_1690675200";

    private static BaJiraWIAggsESQueryBuilder baJiraWIAggsESQueryBuilder;
    private static EsJiraDBHelperService esJiraDBHelperService;
    private static ESClientFactory esClientFactory;

    //region Scaffolding
    @BeforeClass
    public static void startESCreateClient() throws IOException, InterruptedException {
        esContext = esTestUtils.initializeESClient();
        createIndex();
        insertData();

        esClientFactory = EsTestUtils.buildESClientFactory(esContext);

        baJiraWIAggsESQueryBuilder = new BaJiraWIAggsESQueryBuilder(new BATermsESResultConverterFactory(), new BACompositeESResultConverterFactory());

        List<DbJiraField> dbJiraFields = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/db_jira_fields.json"), M.getTypeFactory().constructCollectionType(List.class, DbJiraField.class));

        esJiraDBHelperService = mock(EsJiraDBHelperService.class);
        Mockito.when(esJiraDBHelperService.getDbJiraFields(any(), any(), any())).thenReturn(dbJiraFields);
    }
    @SneakyThrows
    private static void createIndex() {
        String indexTemplate = ResourceUtils.getResourceAsString("index_copy/workitem_index_template.json");
        esContext.getClient().indices().create(new CreateIndexRequest.Builder()
                .index(index)
                .aliases(index + "_a", new Alias.Builder().build())
                .settings(new IndexSettings.Builder().numberOfShards("4").codec("best_compression").maxResultWindow(Integer.MAX_VALUE).build())
                .mappings(TypeMapping.of(s -> s.withJson(new StringReader(indexTemplate))))
                .build());
    }
    @AfterClass
    public static void closeResources() throws Exception {
        esTestUtils.deleteIndex(index);
        esTestUtils.closeResources();
    }
    //endregion
    //region Insert Data
    private static void insertData() throws IOException {
        List<EsWorkItem> allWorkitems = ResourceUtils.getResourceAsList("jira_issues/jira_issues.json", EsWorkItem.class);
        String str = DefaultObjectMapper.get().writeValueAsString(allWorkitems);
        Collections.sort(allWorkitems, (a,b) -> a.getResolvedAt().compareTo(b.getResolvedAt()));
        str = DefaultObjectMapper.get().writeValueAsString(allWorkitems);
        System.out.println(str);
        List<String> res = new ArrayList<>();
        for(EsWorkItem wi : allWorkitems) {
            String cf = wi.getCustomFields().stream().filter(c -> "customfield_22611".equals(c.getName())).map(c -> c.getStrValue()).collect(Collectors.toList()).get(0);
            String val = wi.getAssignee().getDisplayName() + "-" + wi.getStoryPoints() + "-" + cf;
            res.add(val);
        }
        System.out.println(DefaultObjectMapper.get().writeValueAsString(res));
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
    //endregion

    //region Helper Function
    private List<DbAggregationResult> trimResults(DbListResponse<DbAggregationResult> res) {
        return CollectionUtils.emptyIfNull(res.getRecords()).stream().filter(a -> (a.getTotal() != null) && (a.getTotal() > 0) ).collect(Collectors.toList());
    }
    //endregion

    //region Test FTE
    private void testFte(BaJiraAggsESService baJiraAggsESService) throws IOException {
        JiraIssuesFilter jiraIssuesFilter1 = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/jira_issues_filter_1.json"), JiraIssuesFilter.class);
        jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().issueResolutionRange(ImmutablePair.of(1672531199l, 1688169600l)).build();

        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ticket_categorization_filters.json"), M.getTypeFactory().constructCollectionType(List.class, JiraIssuesFilter.TicketCategorizationFilter.class));
        jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().ticketCategorizationFilters(ticketCategorizationFilters).build();

        OUConfiguration ouConfig = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ou_config.json"), OUConfiguration.class);

        List<BaJiraOptions.AttributionMode> attributionModes = List.of(BaJiraOptions.AttributionMode.CURRENT_ASSIGNEE, BaJiraOptions.AttributionMode.CURRENT_AND_PREVIOUS_ASSIGNEES);
        List<JiraAcross> jiraAcrossesWithoutAggsInterval = List.of(JiraAcross.ASSIGNEE, JiraAcross.TICKET_CATEGORY);
        List<JiraAcross> jiraAcrossesWithAggsInterval = List.of(JiraAcross.ISSUE_RESOLVED_AT);
        List<Calculation> calculations = List.of(Calculation.TICKET_COUNT, Calculation.STORY_POINTS, Calculation.TICKET_TIME_SPENT);
        List<String> categories = List.of("KTLO", "Bugs & Escalations", "Security & Compliance", "Other", "LEVELOPS_RESERVED_FOR_TEST");
        List<String> aggIntervals = List.of("week", "biweekly", "month", "quarter");

        //Across With Agg Interval
        for(BaJiraOptions.AttributionMode attributionMode : attributionModes) {
            for(JiraAcross jiraAcross : jiraAcrossesWithAggsInterval) {
                for(Calculation calculation : calculations){
                    for(String aggInterval : aggIntervals) {
                        for(String category : categories) {
                            BaJiraOptions baJiraOptions = BaJiraOptions.builder().attributionMode(attributionMode).build();
                            aggInterval = aggInterval.toString().toLowerCase();
                            List<String> ticketCategories = ("LEVELOPS_RESERVED_FOR_TEST".equals(category)) ? null : List.of(category);
                            JiraIssuesFilter jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(ticketCategories).aggInterval(aggInterval).build();
                            DbListResponse<DbAggregationResult> actual = baJiraAggsESService.doCalculateIssueFTE(company, jiraAcross, calculation, jiraIssuesFilter, null, baJiraOptions, 0, 10000);
                            Assert.assertNotNull(actual);
                            String key = attributionMode + "--" + jiraAcross + "--" + calculation + "--" + aggInterval + "--" + category + ".json";
                            String expectedStr = ResourceUtils.getResourceAsString("ba_jira_aggs_expected/across_with_aggs_interval/" + key);
                            DbListResponse<DbAggregationResult> expected = M.readValue(expectedStr, M.getTypeFactory().constructParametricType(DbListResponse.class, DbAggregationResult.class));
                            Assert.assertEquals(key, trimResults(expected), trimResults(actual));
                        }
                    }
                }
            }
        }

        //Across Without Agg Interval
        for(BaJiraOptions.AttributionMode attributionMode : attributionModes) {
            for(JiraAcross jiraAcross : jiraAcrossesWithoutAggsInterval) {
                for(Calculation calculation : calculations){
                    for(String category : categories) {
                        BaJiraOptions baJiraOptions = BaJiraOptions.builder().attributionMode(attributionMode).build();
                        List<String> ticketCategories = ("LEVELOPS_RESERVED_FOR_TEST".equals(category)) ? null : List.of(category);
                        JiraIssuesFilter jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(ticketCategories).build();
                        DbListResponse<DbAggregationResult> actual = baJiraAggsESService.doCalculateIssueFTE(company, jiraAcross, calculation, jiraIssuesFilter, null, baJiraOptions, 0, 10000);
                        Assert.assertNotNull(actual);
                        String key = attributionMode + "--" + jiraAcross + "--" + calculation + "--" + category + ".json";
                        String expectedStr = ResourceUtils.getResourceAsString("ba_jira_aggs_expected/across_without_aggs_interval/" + key);
                        DbListResponse<DbAggregationResult> expected = M.readValue(expectedStr, M.getTypeFactory().constructParametricType(DbListResponse.class, DbAggregationResult.class));
                        Assert.assertEquals(key, trimResults(expected), trimResults(actual));
                    }
                }
            }
        }
    }
    //endregion

    //region Test List Jira Issues
    private void testJiraIssuesList(BaJiraAggsESService baJiraAggsESService) throws IOException {
        JiraIssuesFilter jiraIssuesFilter1 = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/jira_issues_filter_1.json"), JiraIssuesFilter.class);
        jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().issueResolutionRange(ImmutablePair.of(1672531199l, 1688169600l)).build();

        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ticket_categorization_filters.json"), M.getTypeFactory().constructCollectionType(List.class, JiraIssuesFilter.TicketCategorizationFilter.class));
        jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().ticketCategorizationFilters(ticketCategorizationFilters).build();

        OUConfiguration ouConfig = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ou_config.json"), OUConfiguration.class);

        //Test all categories with large page size
        List<String> categories = List.of("KTLO", "Bugs & Escalations", "Security & Compliance", "Other", "LEVELOPS_RESERVED_FOR_TEST");
        for(String category : categories) {
            List<String> ticketCategories = ("LEVELOPS_RESERVED_FOR_TEST".equals(category)) ? null : List.of(category);
            JiraIssuesFilter jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(ticketCategories).build();
            DbListResponse<DbJiraIssue> actual = baJiraAggsESService.getListOfJiraIssues(company, jiraIssuesFilter, null, 0, 100);
            String key = category + ".json";
            String expectedStr = ResourceUtils.getResourceAsString("ba_jira_list_expected/" + key);
            Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
        }

        //Test one category with small page size
        String category = "Bugs & Escalations";
        JiraIssuesFilter jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(List.of(category)).build();
        int pageSize = 2;
        for (int i =0; i < 2; i++) {
            int pageNumber = i;
            DbListResponse<DbJiraIssue> actual = baJiraAggsESService.getListOfJiraIssues(company, jiraIssuesFilter, null, pageNumber, pageSize);
            String key = category + "-" + "pg_size_" + pageSize + "-pg_" + pageNumber + ".json";
            String expectedStr = ResourceUtils.getResourceAsString("ba_jira_list_expected/" + key);
            Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
        }

        //Test all category with small page size
        category = "LEVELOPS_RESERVED_FOR_TEST";
        jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(null).build();
        pageSize = 10;
        for (int i =0; i < 2; i++) {
            int pageNumber = i;
            DbListResponse<DbJiraIssue> actual = baJiraAggsESService.getListOfJiraIssues(company, jiraIssuesFilter, null, pageNumber, pageSize);
            String key = category + "-" + "pg_size_" + pageSize + "-pg_" + pageNumber + ".json";
            String expectedStr = ResourceUtils.getResourceAsString("ba_jira_list_expected/" + key);
            Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
        }

        //Test all category with really small page size
        category = "LEVELOPS_RESERVED_FOR_TEST";
        jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(null).build();
        pageSize = 2;
        for (int i =0; i < 9; i++) {
            int pageNumber = i;
            DbListResponse<DbJiraIssue> actual = baJiraAggsESService.getListOfJiraIssues(company, jiraIssuesFilter, null, pageNumber, pageSize);
            String key = category + "-small-" + "pg_size_" + pageSize + "-pg_" + pageNumber + ".json";
            String expectedStr = ResourceUtils.getResourceAsString("ba_jira_list_expected/" + key);
            Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
        }
    }
    //endregion

    //region Test Setup Doc
    /*
    Tkt	Mnth    Usr	Cat	SP
    1	Jan	    U1	O	1
    2	Feb	    U2	O	3
    3	Feb	    U3	O	4
    4	Feb	    U1	C1	2
    5	Mar	    U2	C1	6
    6	Mar	    U3	C1	5
    7	Apr	    U1	O	7
    8	Apr	    U2	O	9
    9	Apr	    U3	O	8
    10	May	    U1	C1	11
    11	May	    U2	C2	10
    12	May	    U3	C2	13
    13	May	    U1	O	12
    14	June	U2	O	15
    15	June	U1	C2	14
    16	June	U2	C1	18
    17	June	U1	O	16
    18	June	U1	C1	17
     */
    //endregion
    @Test
    public void tesBAJiraAggs() throws SQLException, IOException {
        BaJiraAggsESService baJiraAggsESServiceComposite = new BaJiraAggsESService(baJiraWIAggsESQueryBuilder, esJiraDBHelperService, esClientFactory, false);
        testJiraIssuesList(baJiraAggsESServiceComposite);
        testFte(baJiraAggsESServiceComposite);
        BaJiraAggsESService baJiraAggsESServiceTerms = new BaJiraAggsESService(baJiraWIAggsESQueryBuilder, esJiraDBHelperService, esClientFactory, true);
        testFte(baJiraAggsESServiceTerms);
    }

}