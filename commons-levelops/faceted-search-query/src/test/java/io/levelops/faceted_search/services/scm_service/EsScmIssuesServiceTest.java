package io.levelops.faceted_search.services.scm_service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.scm.EsScmIssue;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Ignore
public class EsScmIssuesServiceTest {

    private static ESContext esContext;
    private static final EsTestUtils esTestUtils = new EsTestUtils();
    private static EsScmIssuesService esScmIssuesService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String index = "scm_issues_test";
    public static String company = "test";

    @BeforeClass
    public static void startESCreateClient() throws IOException, InterruptedException, SQLException {

        esContext = esTestUtils.initializeESClient();
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        createIndex();
        insertData();

        ESClientFactory esClientFactory = EsTestUtils.buildESClientFactory(esContext);
        esScmIssuesService = new EsScmIssuesService(esClientFactory, dataSource);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    }

    @Test
    public void testAggregation() throws IOException {

        ScmIssueFilter filter = ScmIssueFilter.builder()
                .across(ScmIssueFilter.DISTINCT.repo_id)
                .extraCriteria(List.of())
                .build();

        DbListResponse<DbAggregationResult> res = esScmIssuesService.groupByAndCalculateIssues(company,filter, null, 0, 10);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/faceted-search");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);

        Assertions.assertThat(result.get(2).getKey()).isEqualTo("levelops/devops-levelops");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(2);


        filter = ScmIssueFilter.builder()
                .across(ScmIssueFilter.DISTINCT.creator)
                .extraCriteria(List.of())
                .build();

        res = esScmIssuesService.groupByAndCalculateIssues(company,filter, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);

        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(3);

        filter = ScmIssueFilter.builder()
                .across(ScmIssueFilter.DISTINCT.issue_created)
                .aggInterval(AGG_INTERVAL.month)
                .extraCriteria(List.of())
                .build();

        res = esScmIssuesService.groupByAndCalculateIssues(company,filter, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(2);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("2022-05");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(3);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("2022-06");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(7);

    }


    @Test
    public void testList() throws IOException {

        ScmIssueFilter filter = ScmIssueFilter.builder()
                .creators(List.of("211653ee-5518-40c8-b8fb-b9bdb561bbcf"))
                .integrationIds(List.of("2229"))
                .extraCriteria(List.of())
                .build();

        DbListResponse<DbScmIssue> res = esScmIssuesService.list(company, filter, Map.of(), null, 0,10);
        Assert.assertNotNull(res);
        List<DbScmIssue> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);

        filter = ScmIssueFilter.builder()
                .states(List.of("closed"))
                .projects(List.of("levelops/faceted-search"))
                .extraCriteria(List.of())
                .build();

        res = esScmIssuesService.list(company, filter, Map.of(), null, 0,10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
    }

    @Test
    public void testResponseTime() throws IOException {

        ScmIssueFilter filter = ScmIssueFilter.builder()
                .across(ScmIssueFilter.DISTINCT.repo_id)
                .calculation(ScmIssueFilter.CALCULATION.response_time)
                .extraCriteria(List.of())
                .build();

        DbListResponse<DbAggregationResult> res = esScmIssuesService.groupByAndCalculateIssues(company,filter, null, 0, 10);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(15488000);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(13514000);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(29002000);

    }


    @Test
    public void testResolutionTime() throws IOException {

        ScmIssueFilter filter = ScmIssueFilter.builder()
                .across(ScmIssueFilter.DISTINCT.repo_id)
                .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                .extraCriteria(List.of())
                .build();

        DbListResponse<DbAggregationResult> res = esScmIssuesService.groupByAndCalculateIssues(company,filter, null, 0, 10);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/faceted-search");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(93068000);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(120);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(186538120);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getMax()).isEqualTo(15488000);
        Assertions.assertThat(result.get(1).getMin()).isEqualTo(409000);
        Assertions.assertThat(result.get(1).getSum()).isEqualTo(29398000);

    }


    private static void insertData() throws IOException {

        String data = ResourceUtils.getResourceAsString("data/issues.json");
        List<EsScmIssue> allIssues = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, EsScmIssue.class));

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for(EsScmIssue issue : allIssues) {
            BulkOperation.Builder b = new BulkOperation.Builder();
            b.update(v -> v
                    .index(index)
                    .id(issue.getId())
                    .action(a -> a
                            .docAsUpsert(true)
                            .doc(issue)
                    )
            );
            bulkOperations.add(b.build());
        }
        BulkRequest.Builder bldr = new BulkRequest.Builder();
        bldr.operations(bulkOperations);
        esContext.getClient().bulk(bldr.build());
    }

    @SneakyThrows
    private static void createIndex() {

        String indexTemplate = ResourceUtils.getResourceAsString("index/issues_index_template.json");
        esContext.getClient().indices().create(new CreateIndexRequest.Builder()
                .index(index)
                .aliases(index+"_a", new Alias.Builder().build())
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
