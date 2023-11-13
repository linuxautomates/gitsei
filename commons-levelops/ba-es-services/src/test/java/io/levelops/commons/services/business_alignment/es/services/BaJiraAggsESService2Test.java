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
import io.levelops.commons.databases.models.filters.IssueInheritanceMode;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.services.business_alignment.es.query_builder.BaJiraWIAggsESQueryBuilder;
import io.levelops.commons.services.business_alignment.es.result_converter.composite.BACompositeESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.result_converter.terms.BATermsESResultConverterFactory;
import io.levelops.commons.services.business_alignment.es.utils.EsTestUtils;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class BaJiraAggsESService2Test {
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
        List<EsWorkItem> allWorkitems = ResourceUtils.getResourceAsList("jira_issues/jira_issues_2.json", EsWorkItem.class);
        //Collections.sort(allWorkitems, (a, b) -> a.getResolvedAt().compareTo(b.getResolvedAt()));
        Map<String, EsWorkItem> map = CollectionUtils.emptyIfNull(allWorkitems).stream().collect(Collectors.toMap(x -> x.getWorkitemId(), x -> x));

        EsWorkItem epic = map.get("PROP-1372");
        EsWorkItem sei1179 = map.get("SEI-1179");


        EsWorkItem sei1487 = map.get("SEI-1487");
        EsWorkItem prop1636 = map.get("PROP-1636");
        EsWorkItem sei1248 = map.get("SEI-1248");
        EsWorkItem sei1230 = map.get("SEI-1230");
        EsWorkItem sei1735 = map.get("SEI-1735");
        EsWorkItem prop2658 = map.get("PROP-2658");
        EsWorkItem sei275 = map.get("SEI-275");
        EsWorkItem prop1424 = map.get("PROP-1424");
        EsWorkItem prop1295 = map.get("PROP-1295");

        sei1487 = sei1487.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei1230.toBuilder().parentLevel(0).build(),
                        sei275.toBuilder().parentLevel(1).build(),
                        sei1179.toBuilder().parentLevel(2).build()
                ))
                .build();

        prop1636 = prop1636.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei1230.toBuilder().parentLevel(0).build(),
                        sei275.toBuilder().parentLevel(1).build(),
                        sei1179.toBuilder().parentLevel(2).build()
                ))
                .build();

        sei1248 = sei1248.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei1230.toBuilder().parentLevel(0).build(),
                        sei275.toBuilder().parentLevel(1).build(),
                        sei1179.toBuilder().parentLevel(2).build()
                ))
                .build();

        sei1230 = sei1230.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei275.toBuilder().parentLevel(0).build(),
                        sei1179.toBuilder().parentLevel(1).build()
                ))
                .build();

        sei1735 = sei1735.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei275.toBuilder().parentLevel(0).build(),
                        sei1179.toBuilder().parentLevel(1).build()
                ))
                .build();

        sei275 = sei275.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei1179.toBuilder().parentLevel(0).build()
                ))
                .build();

        prop2658 = prop2658.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        prop1424.toBuilder().parentLevel(0).build(),
                        sei1179.toBuilder().parentLevel(1).build()
                ))
                .build();

        prop1424 = prop1424.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei1179.toBuilder().parentLevel(0).build()
                ))
                .build();

        prop1295 = prop1295.toBuilder()
                .epicWorkItem(epic)
                .parents(List.of(
                        sei1179.toBuilder().parentLevel(0).build()
                ))
                .build();
        List<EsWorkItem> allUpdatedWorkitems = new ArrayList<>();
        allUpdatedWorkitems.add(epic);
        allUpdatedWorkitems.add(sei1179);
        allUpdatedWorkitems.add(sei1487);
        allUpdatedWorkitems.add(prop1636);
        allUpdatedWorkitems.add(sei1248);
        allUpdatedWorkitems.add(sei1230);
        allUpdatedWorkitems.add(sei1735);
        allUpdatedWorkitems.add(prop2658);
        allUpdatedWorkitems.add(sei275);
        allUpdatedWorkitems.add(prop1424);
        allUpdatedWorkitems.add(prop1295);

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (EsWorkItem wi : allUpdatedWorkitems) {
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

    //region Test List Jira Issues
    private void testJiraIssuesList(BaJiraAggsESService baJiraAggsESService) throws IOException {
        JiraIssuesFilter jiraIssuesFilter1 = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/jira_issues_filter_1.json"), JiraIssuesFilter.class);
        jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().issueResolutionRange(ImmutablePair.of(1672531199l, 1688169600l)).build();

        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ticket_categorization_filters.json"), M.getTypeFactory().constructCollectionType(List.class, JiraIssuesFilter.TicketCategorizationFilter.class));


        OUConfiguration ouConfig = M.readValue(ResourceUtils.getResourceAsString("tc1_equifax/ou_config.json"), OUConfiguration.class);

        //Test all categories with large page size
        List<String> categories = List.of("KTLO", "Bugs & Escalations", "Security & Compliance", "Other", "LEVELOPS_RESERVED_FOR_TEST");
        for(String category : categories) {
            for(IssueInheritanceMode inheritanceMode : IssueInheritanceMode.values()) {
                //ToDo: VA Fix
                //List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFiltersWithInheritance = ticketCategorizationFilters.stream().map(f -> f.toBuilder().issueInheritanceMode(inheritanceMode).build()).collect(Collectors.toList());
                List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFiltersWithInheritance = ticketCategorizationFilters.stream().map(f -> f.toBuilder().build()).collect(Collectors.toList());
                jiraIssuesFilter1 = jiraIssuesFilter1.toBuilder().ticketCategorizationFilters(ticketCategorizationFiltersWithInheritance).build();

                List<String> ticketCategories = ("LEVELOPS_RESERVED_FOR_TEST".equals(category)) ? null : List.of(category);
                JiraIssuesFilter jiraIssuesFilter = jiraIssuesFilter1.toBuilder().ticketCategories(ticketCategories).build();
                DbListResponse<DbJiraIssue> actual = baJiraAggsESService.getListOfJiraIssues(company, jiraIssuesFilter, null, 0, 100);
                String key = category + ".json";
                String expectedStr = ResourceUtils.getResourceAsString("ba_jira_list_expected/" + key);
                //Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
            }
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
            //Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
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
            //Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
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
            //Assert.assertEquals(expectedStr, DefaultObjectMapper.get().writeValueAsString(actual.getRecords()));
        }
    }
    //endregion
    @Test
    public void test() throws IOException {
        BaJiraAggsESService baJiraAggsESServiceComposite = new BaJiraAggsESService(baJiraWIAggsESQueryBuilder, esJiraDBHelperService, esClientFactory, false);
        testJiraIssuesList(baJiraAggsESServiceComposite);
    }
}